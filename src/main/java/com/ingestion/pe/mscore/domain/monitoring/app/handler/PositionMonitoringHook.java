package com.ingestion.pe.mscore.domain.monitoring.app.handler;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.atu.app.dispatcher.AtuTransmissionAsyncDispatcher;
import com.ingestion.pe.mscore.domain.monitoring.core.calc.DelayCalculator;
import com.ingestion.pe.mscore.domain.monitoring.core.calc.HaversineCalculator;
import com.ingestion.pe.mscore.domain.monitoring.core.model.ControlPointModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripActiveResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PositionMonitoringHook {

    private static final double CONTROL_POINT_RADIUS_METERS = 100.0;
    private static final double MAX_BUS_SPEED_KMH = 120.0;

    private final TripStateManager tripStateManager;
    private final RouteConfigClient routeConfigClient;
    private final DelayCalculator delayCalculator;
    private final KafkaPublisherService kafkaPublisherService;
    private final MonitoringCachePublisher monitoringCachePublisher;
    private final AtuTransmissionAsyncDispatcher atuTransmissionAsyncDispatcher;

    private record PositionTask(Long deviceId, double lat, double lon, double speedKmh, Instant time, Position position, Long companyId) {}

    private final ConcurrentHashMap<Long, AtomicReference<PositionTask>> pendingByDevice = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicBoolean> busyByDevice = new ConcurrentHashMap<>();

    public void onPositionReceived(Long deviceId, double lat, double lon, double speedKmh, Instant time, Position position, Long companyId) {
        AtomicReference<PositionTask> pendingRef = pendingByDevice.computeIfAbsent(deviceId, k -> new AtomicReference<>());
        AtomicBoolean busyFlag = busyByDevice.computeIfAbsent(deviceId, k -> new AtomicBoolean(false));

        pendingRef.set(new PositionTask(deviceId, lat, lon, speedKmh, time, position, companyId));

        if (!busyFlag.compareAndSet(false, true)) {
            log.debug("PositionMonitoringHook: device={} ocupado (latest-wins), posición registrada como pending", deviceId);
            return;
        }

        try {
            PositionTask task;
            while ((task = pendingRef.getAndSet(null)) != null) {
                processPositionInternal(task);
            }
        } finally {
            busyFlag.set(false);
            if (pendingRef.get() != null && busyFlag.compareAndSet(false, true)) {
                try {
                    PositionTask task;
                    while ((task = pendingRef.getAndSet(null)) != null) {
                        processPositionInternal(task);
                    }
                } finally {
                    busyFlag.set(false);
                }
            }
        }
    }

    private void processPositionInternal(PositionTask task) {
        try {
            Long deviceId = task.deviceId();
            double lat = task.lat();
            double lon = task.lon();
            double speedKmh = task.speedKmh();
            Instant time = task.time();

            Optional<TripState> existingState = findStateByDeviceContext(deviceId);

            if (existingState.isEmpty()) {
                Optional<Long> vehicleIdOpt = resolveVehicleId(deviceId);
                if (vehicleIdOpt.isEmpty()) {
                    return;
                }

                Optional<Long> activeTripIdOpt = routeConfigClient.getActiveTripIdForVehicle(vehicleIdOpt.get());
                if (activeTripIdOpt.isEmpty()) {
                    return;
                }

                Optional<TripActiveResponse> tripOpt = routeConfigClient.getActiveTrip(activeTripIdOpt.get());
                if (tripOpt.isEmpty()) {
                    routeConfigClient.evictActiveTripIdForVehicle(vehicleIdOpt.get());
                    return;
                }

                initializeTripState(tripOpt.get());
                existingState = tripStateManager.getStateByVehicleId(vehicleIdOpt.get());
                if (existingState.isEmpty()) {
                    return;
                }
            }

            TripState state = existingState.get();

            Optional<RouteConfigResponse> configOpt = routeConfigClient.getRouteConfig(state.getRouteId());
            if (configOpt.isEmpty()) {
                log.debug("No se encontró configuración de ruta {} en Redis", state.getRouteId());
                return;
            }

            RouteConfigResponse config = configOpt.get();

            if ("CIRCULAR".equalsIgnoreCase(config.getType()) && !"LOOP".equalsIgnoreCase(state.getDirection())) {
                state.setDirection("LOOP");
            }

            List<ControlPointModel> controlPoints = config.getControlPoints();

            if (controlPoints == null || controlPoints.isEmpty()) {
                return;
            }

            double jumpKm = (state.getLastLatitude() != null && state.getLastLongitude() != null)
                    ? HaversineCalculator.distanceKm(state.getLastLatitude(), state.getLastLongitude(), lat, lon)
                    : 0.0;
            double deltaHours = Duration.between(state.getLastUpdateTime(), time).toMillis() / 3600000.0;
            boolean isAnomaly = false;

            if (speedKmh > MAX_BUS_SPEED_KMH) {
                log.warn("GPS anomalía detectada (Velocidad Nativa): tripId={}, speedKmh={}",
                        state.getTripId(), String.format("%.1f", speedKmh));
                isAnomaly = true;
            } else if (deltaHours > 0) {
                double calculatedSpeedKmh = jumpKm / deltaHours;
                if (calculatedSpeedKmh > (MAX_BUS_SPEED_KMH + 30.0)) {
                    log.warn("GPS anomalía detectada (Salto Haversine): tripId={}, calcSpeed={}",
                            state.getTripId(), String.format("%.1f", calculatedSpeedKmh));
                    isAnomaly = true;
                }
            }

            if (isAnomaly) {
                state.setLastUpdateTime(time);
                tripStateManager.updateState(state.getTripId(), state);
                monitoringCachePublisher.processRouteThrottled(state.getRouteId());
                return;
            }

            int currentIndex = state.getCurrentPointIndex();
            int newIndex = detectControlPointCrossing(lat, lon, controlPoints, currentIndex);

            double accumulatedDistance = calculateAccumulatedDistance(
                    lat, lon, controlPoints, newIndex);

            state.setCurrentPointIndex(newIndex);
            state.setAccumulatedDistanceKm(accumulatedDistance);
            state.setLastLatitude(lat);
            state.setLastLongitude(lon);
            state.setLastUpdateTime(time);
            state.setLastSpeedKmh(speedKmh);

            delayCalculator.evaluate(state, config);

            tripStateManager.updateState(state.getTripId(), state);

            monitoringCachePublisher.processRouteThrottled(state.getRouteId());
            
            atuTransmissionAsyncDispatcher.dispatchAsync(task.position(), state);

        } catch (Exception e) {
            log.error("Error en processPositionInternal para deviceId={}: {}", task.deviceId(), e.getMessage());
        }
    }

    public void initializeTripState(TripActiveResponse trip) {
        TripState state = TripState.builder()
                .tripId(trip.getId())
                .routeId(trip.getRouteId())
                .vehicleId(trip.getVehicleId())
                .companyId(trip.getCompanyId())
                .driverId(trip.getDriverId())
                .driverDocumentNumber(trip.getDriverDocumentNumber())
                .atuRouteCode(trip.getAtuRouteCode())
                .direction(trip.getDirection())
                .currentPointIndex(0)
                .accumulatedDistanceKm(0.0)
                .lastUpdateTime(Instant.now())
                .dispatchTime(trip.getDispatchTime())
                .accumulatedDelayMinutes(0.0)
                .status("ON_TIME")
                .build();

        tripStateManager.updateState(trip.getId(), state);
        log.info("TripState inicializado para tripId={}, vehicleId={}", trip.getId(), trip.getVehicleId());
    }

    private Optional<TripState> findStateByDeviceContext(Long deviceId) {
        Optional<Long> vehicleIdOpt = resolveVehicleId(deviceId);
        if (vehicleIdOpt.isEmpty()) {
            return Optional.empty();
        }
        return tripStateManager.getStateByVehicleId(vehicleIdOpt.get());
    }

    private Optional<Long> resolveVehicleId(Long deviceId) {
        return routeConfigClient.getVehicleIdByDeviceId(deviceId);
    }

    private int detectControlPointCrossing(double lat, double lon,
            List<ControlPointModel> controlPoints, int currentIndex) {

        int newIndex = currentIndex;

        for (int i = currentIndex + 1; i < controlPoints.size(); i++) {
            ControlPointModel cp = controlPoints.get(i);
            if (HaversineCalculator.isWithinRadius(lat, lon,
                    cp.getLatitude(), cp.getLongitude(), CONTROL_POINT_RADIUS_METERS)) {
                newIndex = i;
            }
        }

        return newIndex;
    }

    private double calculateAccumulatedDistance(double lat, double lon,
            List<ControlPointModel> controlPoints, int currentIndex) {

        if (currentIndex < 0 || currentIndex >= controlPoints.size()) {
            return 0.0;
        }

        ControlPointModel currentCP = controlPoints.get(currentIndex);
        double baseDistance = currentCP.getDistanceFromStartKm() != null
                ? currentCP.getDistanceFromStartKm()
                : 0.0;

        double segmentDistance = HaversineCalculator.distanceKm(
                currentCP.getLatitude(), currentCP.getLongitude(), lat, lon);

        if (currentIndex + 1 < controlPoints.size()) {
            ControlPointModel nextCP = controlPoints.get(currentIndex + 1);
            double maxSegment = HaversineCalculator.distanceKm(
                    currentCP.getLatitude(), currentCP.getLongitude(),
                    nextCP.getLatitude(), nextCP.getLongitude());
            segmentDistance = Math.min(segmentDistance, maxSegment);
        }

        return baseDistance + segmentDistance;
    }

}
