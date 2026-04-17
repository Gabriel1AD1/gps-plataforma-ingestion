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

    public void evaluateAndTransmit(Position position, TripState tripState) {
        try {
            Long companyId = tripState.getCompanyId();
            Optional<AtuTokenCache> configOpt = atuConfigPort.getAtuConfig(companyId);
            if (configOpt.isEmpty()) {
                log.info("ATU skip: companyId={} sin token ATU configurado en Redis/DB", companyId);
                return;
            }
            AtuTokenCache config = configOpt.get();
            if (config.getToken() == null || config.getToken().isBlank()) {
                log.warn("ATU skip: Token ATU vacío o nulo para companyId={} (Verificar configuración)", companyId);
                return;
            }


            Instant deviceTime = (position.getDeviceTime() != null)
                    ? position.getDeviceTime().toInstant()
                    : Instant.now();
            Instant nowTime = Instant.now();
            long staleMinutes = Duration.between(deviceTime, nowTime).toMinutes();

            log.info("[ATU DEBUG] Evaluación de tiempos: IMEI={} | DeviceTime={} | PositionServerTime={} | SystemNow={} | Antigüedad={}min",
                    position.getImei(), deviceTime, position.getServerTime(), nowTime, staleMinutes);

            if (staleMinutes > STALE_THRESHOLD_MINUTES) {
                log.warn("ATU trama descartada por ANTIGÜEDAD: IMEI={} antigüedad={}min (>{}min). DeviceTime={} Now={}",
                        position.getImei(), staleMinutes, STALE_THRESHOLD_MINUTES, deviceTime, nowTime);
                return;
            }


            String imei = position.getImei();
            Instant lastSent = lastTransmissionByImei.get(imei);
            Instant now = Instant.now();
            if (lastSent != null && Duration.between(lastSent, now).getSeconds() < RATE_LIMIT_SECONDS) {
                log.info("ATU rate limit: IMEI={} ({}s desde último envío)",
                        imei, Duration.between(lastSent, now).getSeconds());
                return;
            }

            String routeId = resolveRouteId(tripState);
            int directionId = mapDirection(tripState.getDirection());

            long ts = (Instant.now().toEpochMilli() / 1000) * 1000;

            long tsInitialTrip = (tripState.getDispatchTime() != null)
                    ? (tripState.getDispatchTime().toEpochMilli() / 1000) * 1000
                    : ts;

            if (tsInitialTrip > ts + 60_000L) {
                log.warn("[ATU] tsinitialtrip futuro detectado ({}), usando ts actual ({})", tsInitialTrip, ts);
                tsInitialTrip = ts;
            }

            String rawLicensePlate = routeConfigClient
                    .getVehiclePlate(tripState.getVehicleId())
                    .orElse("UNKNOWN");


            String licensePlate = rawLicensePlate.replace("-", "").toUpperCase();

            String driverDoi = Optional.ofNullable(tripState.getDriverDocumentNumber())
                    .filter(doi -> !doi.isBlank())
                    .orElseGet(() -> Optional.ofNullable(tripState.getDriverId())
                            .flatMap(driverDataPort::getDriverDocumentNumber)
                            .orElse(DEFAULT_DRIVER_DOI));

            String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            if (driverDoi == null || driverDoi.equalsIgnoreCase("desconocido") || driverDoi.isBlank()) {
                driverDoi = DEFAULT_DRIVER_DOI;
            }

            AtuPayload payload = AtuPayload.builder()
                    .imei(imei)
                    .latitude(round(position.getLatitude(), 6))
                    .longitude(round(position.getLongitude(), 6))
                    .routeId(routeId)
                    .ts(ts)
                    .licensePlate(truncate(licensePlate, 7))
                    .speed(round(position.getSpeedInKm(), 2))
                    .directionId(directionId)
                    .driverId(driverDoi)
                    .tsInitialTrip(tsInitialTrip)
                    .identifier(identifier)
                    .build();

            if (!isValidPayload(payload)) {
                log.warn("ATU skip: Payload inválido no cumple con el formato ATU. IMEI={} Payload={}", imei, payload);
                return;
            }

            log.info("[ATU] Preparando JSON para envío: IMEI={}, Placa={}, Ruta={}",
                    payload.getImei(), payload.getLicensePlate(), payload.getRouteId());
            
            boolean sent = atuWebSocketPort.sendPayload(config.getToken(), config.getEndpoint(), payload);

            if (sent) {
                lastTransmissionByImei.put(imei, now);
                log.debug("ATU trama enviada: IMEI={} routeId={} direction={} driverId={}",
                        imei, routeId, directionId, driverDoi);
            }

        } catch (Exception e) {
            log.error("ATU error inesperado transmitiendo para IMEI={}: {}",
                    position.getImei(), e.getMessage(), e);
        }
    }

    private String resolveRouteId(TripState tripState) {
        if (tripState.getAtuRouteCode() != null && !tripState.getAtuRouteCode().isBlank()) {
            return tripState.getAtuRouteCode();
        }

        return Optional.ofNullable(tripState.getRouteId())
                .flatMap(routeConfigClient::getRouteConfig)
                .map(config -> config.getAtuRouteCode() != null
                        ? config.getAtuRouteCode()
                        : String.valueOf(tripState.getRouteId()))
                .orElseGet(() -> String.valueOf(tripState.getRouteId()));
    }

    private int mapDirection(String direction) {
        if (direction == null)
            return 0;
        return switch (direction.toUpperCase()) {
            case "INBOUND" -> 1;
            default -> 0;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private boolean isValidPayload(AtuPayload payload) {
        if (payload.getImei() == null || !payload.getImei().matches("^[0-9]{15}$"))
            return false;
        if (payload.getLicensePlate() == null || !payload.getLicensePlate().matches("^[a-zA-Z0-9]{1,7}$"))
            return false;
        if (payload.getLatitude() < -90 || payload.getLatitude() > 90)
            return false;
        if (payload.getLongitude() < -180 || payload.getLongitude() > 180)
            return false;
        if (payload.getSpeed() < 0 || payload.getSpeed() > 999.99)
            return false;
        if (payload.getRouteId() == null || payload.getRouteId().length() > 10
                || !payload.getRouteId().matches("^[a-zA-Z0-9]+$"))
            return false;
        if (payload.getDirectionId() != 0 && payload.getDirectionId() != 1)
            return false;
        if (payload.getDriverId() == null || payload.getDriverId().length() > 20
                || !payload.getDriverId().matches("^[a-zA-Z0-9]+$"))
            return false;
        if (payload.getIdentifier() == null || payload.getIdentifier().isBlank()
                || payload.getIdentifier().length() > 50 || !payload.getIdentifier().matches("^[a-zA-Z0-9]+$"))
            return false;

        return true;
    }
}