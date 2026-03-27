package com.ingestion.pe.mscore.domain.monitoring.app.handler;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
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

    public void onPositionReceived(Long deviceId, double lat, double lon, double speedKmh, Instant time) {
        try {
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
                    return;
                }

                initializeTripState(tripOpt.get());
                existingState = tripStateManager.getStateByVehicleId(vehicleIdOpt.get());
                if (existingState.isEmpty()) {
                    return;
                }
            }

            TripState state = existingState.get();

            // Obtener configuración de la ruta (control points)
            Optional<RouteConfigResponse> configOpt = routeConfigClient.getRouteConfig(state.getRouteId());
            if (configOpt.isEmpty()) {
                log.debug("No se encontró configuración de ruta {} en Redis", state.getRouteId());
                return;
            }

            RouteConfigResponse config = configOpt.get();
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
                publishTripStateUpdate(state);
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

            publishTripStateUpdate(state);

        } catch (Exception e) {
            log.error("Error en PositionMonitoringHook para deviceId={}: {}", deviceId, e.getMessage());
        }
    }

    public void initializeTripState(TripActiveResponse trip) {
        TripState state = TripState.builder()
                .tripId(trip.getId())
                .routeId(trip.getRouteId())
                .vehicleId(trip.getVehicleId())
                .driverId(trip.getDriverId())
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
        // El RouteConfigClient puede resolver vehicleId desde deviceId via Redis
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

    private void publishTripStateUpdate(TripState state) {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("tripId", state.getTripId());
            properties.put("routeId", state.getRouteId());
            properties.put("vehicleId", state.getVehicleId());
            properties.put("currentPointIndex", state.getCurrentPointIndex());
            properties.put("accumulatedDistanceKm", state.getAccumulatedDistanceKm());
            properties.put("accumulatedDelayMinutes", state.getAccumulatedDelayMinutes());
            properties.put("status", state.getStatus());
            properties.put("direction", state.getDirection());
            if (state.getLastLatitude() != null) {
                properties.put("latitude", state.getLastLatitude());
            }
            if (state.getLastLongitude() != null) {
                properties.put("longitude", state.getLastLongitude());
            }
            if (state.getLastSpeedKmh() != null) {
                properties.put("speedKmh", state.getLastSpeedKmh());
            }

            WebsocketMessage message = WebsocketMessage.builder()
                    .broadcast(true)
                    .messageAgregateType(WebsocketMessage.MessageAgregateType.TRIP_STATE_UPDATE)
                    .messageType(WebsocketMessage.MessageType.REFRESH)
                    .properties(properties)
                    .build();

            kafkaPublisherService.publishWebsocketMessage(message);
        } catch (Exception e) {
            log.warn("Error publicando TripState update por WebSocket: {}", e.getMessage());
        }
    }
}
