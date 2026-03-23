package com.ingestion.pe.mscore.domain.atu.app.service;

import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.atu.core.model.AtuPayload;
import com.ingestion.pe.mscore.domain.atu.model.AtuTokenCache;
import com.ingestion.pe.mscore.domain.atu.app.port.AtuConfigPort;
import com.ingestion.pe.mscore.domain.atu.app.port.AtuWebSocketPort;
import com.ingestion.pe.mscore.domain.atu.app.port.DriverDataPort;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtuTransmissionUseCase {

    private static final int STALE_THRESHOLD_MINUTES = 10;
    private static final int RATE_LIMIT_SECONDS = 20;
    private static final String DEFAULT_DRIVER_DOI = "00000000";

    private final AtuConfigPort atuConfigPort;
    private final DriverDataPort driverDataPort;
    private final AtuWebSocketPort atuWebSocketPort;
    private final TripStateManager tripStateManager;
    private final RouteConfigClient routeConfigClient;

    private final ConcurrentHashMap<String, Instant> lastTransmissionByImei = new ConcurrentHashMap<>();

    public void evaluateAndTransmit(Position position, Long deviceId, Long companyId) {
        try {
            Optional<AtuTokenCache> configOpt = atuConfigPort.getAtuConfig(companyId);
            if (configOpt.isEmpty()) {
                log.trace("ATU skip: companyId={} sin token ATU configurado", companyId);
                return;
            }
            AtuTokenCache config = configOpt.get();

            Optional<Long> vehicleIdOpt = routeConfigClient.getVehicleIdByDeviceId(deviceId);
            if (vehicleIdOpt.isEmpty()) {
                log.trace("ATU skip: deviceId={} sin vehículo asociado en caché", deviceId);
                return;
            }

            Optional<TripState> tripStateOpt = tripStateManager.getStateByVehicleId(vehicleIdOpt.get());
            if (tripStateOpt.isEmpty()) {
                log.trace("ATU skip: vehicleId={} sin viaje activo (TripState)", vehicleIdOpt.get());
                return;
            }
            TripState tripState = tripStateOpt.get();

            Instant deviceTime = (position.getDeviceTime() != null)
                    ? position.getDeviceTime().toInstant()
                    : Instant.now();
            long staleMinutes = Duration.between(deviceTime, Instant.now()).toMinutes();
            if (staleMinutes > STALE_THRESHOLD_MINUTES) {
                log.warn("ATU trama descartada: IMEI={} antigüedad={}min (>{}min). Pendiente para envío REST Lote cuando ATU habilite el endpoint.",
                        position.getImei(), staleMinutes, STALE_THRESHOLD_MINUTES);
                return;
            }

            String imei = position.getImei();
            Instant lastSent = lastTransmissionByImei.get(imei);
            Instant now = Instant.now();
            if (lastSent != null && Duration.between(lastSent, now).getSeconds() < RATE_LIMIT_SECONDS) {
                log.trace("ATU skip: IMEI={} rate limit activo ({}s desde último envío)",
                        imei, Duration.between(lastSent, now).getSeconds());
                return;
            }

            String routeId = resolveRouteId(tripState);
            int directionId = mapDirection(tripState.getDirection());
            long ts = deviceTime.toEpochMilli();

            long tsInitialTrip = (tripState.getDispatchTime() != null)
                    ? tripState.getDispatchTime().toEpochMilli()
                    : ts;

            String licensePlate = routeConfigClient
                    .getVehicleIdByDeviceId(deviceId)
                    .flatMap(vId -> routeConfigClient.getVehiclePlate(vId))
                    .orElse("UNKNOWN");

            String driverDoi = Optional.ofNullable(tripState.getDriverId())
                    .flatMap(driverDataPort::getDriverDocumentNumber)
                    .orElse(DEFAULT_DRIVER_DOI);

            String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            AtuPayload payload = AtuPayload.builder()
                    .imei(imei)
                    .latitude(position.getLatitude())
                    .longitude(position.getLongitude())
                    .routeId(routeId)
                    .ts(ts)
                    .licensePlate(truncate(licensePlate, 7))
                    .speed(position.getSpeedInKm())
                    .directionId(directionId)
                    .driverId(driverDoi)
                    .tsInitialTrip(tsInitialTrip)
                    .identifier(identifier)
                    .build();

            if (!isValidPayload(payload)) {
                log.warn("ATU skip: Payload inválido no cumple con el formato ATU. IMEI={}", imei);
                return;
            }

            atuWebSocketPort.sendPayload(config.getToken(), config.getEndpoint(), payload);

            lastTransmissionByImei.put(imei, now);

            log.debug("ATU trama enviada: IMEI={} routeId={} direction={} driverId={}",
                    imei, routeId, directionId, driverDoi);

        } catch (Exception e) {
            log.error("ATU error inesperado transmitiendo para IMEI={}: {}",
                    position.getImei(), e.getMessage(), e);
        }
    }

    private String resolveRouteId(TripState tripState) {
        return Optional.ofNullable(tripState.getRouteId())
                .flatMap(routeConfigClient::getRouteConfig)
                .map(config -> config.getAtuRouteCode() != null
                        ? config.getAtuRouteCode()
                        : String.valueOf(tripState.getRouteId()))
                .orElseGet(() -> String.valueOf(tripState.getRouteId()));
    }

    private int mapDirection(String direction) {
        if (direction == null) return 0;
        return switch (direction.toUpperCase()) {
            case "INBOUND" -> 1;
            default -> 0;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private boolean isValidPayload(AtuPayload payload) {
        if (payload.getImei() == null || !payload.getImei().matches("^[a-zA-Z0-9]{15}$")) return false;
        if (payload.getLicensePlate() == null || !payload.getLicensePlate().matches("^[a-zA-Z0-9-]{1,7}$")) return false;
        if (payload.getLatitude() < -90 || payload.getLatitude() > 90) return false;
        if (payload.getLongitude() < -180 || payload.getLongitude() > 180) return false;
        if (payload.getSpeed() < 0 || payload.getSpeed() > 999.99) return false;
        if (payload.getRouteId() == null || payload.getRouteId().length() > 10 || !payload.getRouteId().matches("^[a-zA-Z0-9]+$")) return false;
        if (payload.getDirectionId() != 0 && payload.getDirectionId() != 1) return false;
        if (payload.getDriverId() == null || payload.getDriverId().length() > 20 || !payload.getDriverId().matches("^[a-zA-Z0-9]+$")) return false;
        if (payload.getIdentifier() == null || payload.getIdentifier().isBlank() || payload.getIdentifier().length() > 50 || !payload.getIdentifier().matches("^[a-zA-Z0-9]+$")) return false;
        
        return true;
    }
}