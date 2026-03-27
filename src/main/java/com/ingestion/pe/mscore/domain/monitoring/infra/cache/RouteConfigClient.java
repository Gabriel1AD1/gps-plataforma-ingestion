package com.ingestion.pe.mscore.domain.monitoring.infra.cache;

import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripActiveResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteConfigClient {

    private final CacheDao<RouteConfigResponse> routeConfigCacheDao;
    private final CacheDao<TripActiveResponse> tripActiveCacheDao;
    private final CacheDao<Long[]> tripIdsCacheDao;
    private final CacheDao<VehicleResponse> vehicleCacheDao;
    private final com.ingestion.pe.mscore.domain.vehicles.core.repo.VehicleReadEntityRepository vehicleReadEntityRepository;
    private final com.ingestion.pe.mscore.domain.dispatch.core.repo.TripReadEntityRepository tripReadEntityRepository;

    public Optional<RouteConfigResponse> getRouteConfig(Long routeId) {
        return routeConfigCacheDao.get("route:config:" + routeId, RouteConfigResponse.class);
    }

    // Trip activo individual con fallback a DB
    public Optional<TripActiveResponse> getActiveTrip(Long tripId) {
        Optional<TripActiveResponse> cached = tripActiveCacheDao.get("trip:active:" + tripId, TripActiveResponse.class);
        if (cached.isPresent()) {
            return cached;
        }
        log.warn("[REDIS] Trip {} no encontrado en cache. Intentando fallback a DB.", tripId);
        return tripReadEntityRepository.findById(tripId)
                .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .map(entity -> {
                    TripActiveResponse response = mapToTripActiveResponse(entity);
                    tripActiveCacheDao.save("trip:active:" + tripId, response, 3600);
                    return response;
                });
    }

    // TripId activo para un vehículo especifico con fallback a DB
    public Optional<Long> getActiveTripIdForVehicle(Long vehicleId) {
        Optional<Long> cached = tripIdsCacheDao
                .get("trip:active:vehicle:" + vehicleId, Long[].class)
                .map(arr -> arr.length > 0 ? arr[0] : null);
        
        if (cached.isPresent()) {
            return cached;
        }

        log.warn("[REDIS] TripId para Vehiculo {} no encontrado en cache. Fallback a DB.", vehicleId);
        return tripReadEntityRepository.findFirstByVehicleIdAndStatus(vehicleId, "ACTIVE")
                .map(entity -> {
                    Long tId = entity.getId();
                    tripIdsCacheDao.save("trip:active:vehicle:" + vehicleId, new Long[] { tId }, 3600);
                    return tId;
                });
    }

    // Lista de tripIds activos para una ruta con fallback a DB
    public List<Long> getActiveTripsForRoute(Long routeId) {
        Optional<List<Long>> cached = tripIdsCacheDao
                .get("trip:active:route:" + routeId, Long[].class)
                .map(Arrays::asList);

        if (cached.isPresent()) {
            return cached.get();
        }

        log.warn("[REDIS] Trips para Ruta {} no encontrados en cache. Fallback a DB.", routeId);
        List<Long> ids = tripReadEntityRepository.findAllByRouteIdAndStatus(routeId, "ACTIVE")
                .stream()
                .map(com.ingestion.pe.mscore.domain.dispatch.core.entity.TripReadEntity::getId)
                .toList();
        
        if (!ids.isEmpty()) {
            tripIdsCacheDao.save("trip:active:route:" + routeId, ids.toArray(new Long[0]), 3600);
        }
        
        return ids;
    }

    private TripActiveResponse mapToTripActiveResponse(com.ingestion.pe.mscore.domain.dispatch.core.entity.TripReadEntity entity) {
        return TripActiveResponse.builder()
                .id(entity.getId())
                .routeId(entity.getRouteId())
                .vehicleId(entity.getVehicleId())
                .companyId(entity.getCompanyId())
                .direction(entity.getDirection())
                .lapNumber(entity.getLapNumber())
                .status(entity.getStatus())
                .dispatchTime(entity.getDispatchTime())
                .departureTerminalId(entity.getDepartureTerminalId())
                .arrivalTerminalId(entity.getArrivalTerminalId())
                .build();
    }

    public Optional<Long> getVehicleIdByDeviceId(Long deviceId) {
        String key = "vehicle:deviceId:" + deviceId;
        log.info("[REDIS] Buscando Vehículo para DeviceID={} con clave {}", deviceId, key);
        Optional<VehicleResponse> cached = vehicleCacheDao.get(key, VehicleResponse.class);
        
        if (cached.isPresent()) {
            log.info("[REDIS] Vehículo encontrado en cache: ID={}", cached.get().getId());
            return cached.map(VehicleResponse::getId);
        }
        
        log.warn("[REDIS] No se encontrado en cache. Intentando fallback a BASE DE DATOS para DeviceID={}", deviceId);
        return vehicleReadEntityRepository.findByDeviceId(deviceId)
                .map(entity -> {
                    log.info("[DB-FALLBACK] Vehículo encontrado en DB: ID={}", entity.getId());
                    VehicleResponse response = mapToVehicleResponse(entity);
                    vehicleCacheDao.save("vehicle:id:" + entity.getId(), response, 3600);
                    vehicleCacheDao.save("vehicle:deviceId:" + deviceId, response, 3600);
                    return entity.getId();
                });
    }

    public Optional<String> getVehiclePlate(Long vehicleId) {
        Optional<String> cached = vehicleCacheDao
                .get("vehicle:id:" + vehicleId, VehicleResponse.class)
                .map(VehicleResponse::getLicensePlate);
        
        if (cached.isPresent()) {
            return cached;
        }

        log.warn("[REDIS] Placa para Vehiculo {} no encontrada en cache. Fallback a DB.", vehicleId);
        return vehicleReadEntityRepository.findById(vehicleId)
                .map(entity -> {
                    VehicleResponse response = mapToVehicleResponse(entity);
                    vehicleCacheDao.save("vehicle:id:" + vehicleId, response, 3600);
                    return entity.getLicensePlate();
                });
    }

    public List<Long> getKnownRouteIds() {
        Optional<List<Long>> cached = tripIdsCacheDao
                .get("route:active:ids", Long[].class)
                .map(Arrays::asList);

        if (cached.isPresent()) {
            return cached.get();
        }

        log.warn("[REDIS] Lista de rutas activas no encontrada en cache. Fallback a DB.");
        List<Long> ids = tripReadEntityRepository.findAllActiveRouteIds();
        
        if (!ids.isEmpty()) {
            tripIdsCacheDao.save("route:active:ids", ids.toArray(new Long[0]), 3600);
        }
        
        return ids;
    }

    private VehicleResponse mapToVehicleResponse(com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleReadEntity entity) {
        return VehicleResponse.builder()
                .id(entity.getId())
                .deviceId(entity.getDeviceId())
                .companyId(entity.getCompanyId())
                .licensePlate(entity.getLicensePlate())
                .brand(entity.getBrand())
                .model(entity.getModel())
                .year(entity.getYear())
                .color(entity.getColor())
                .status(entity.getStatus())
                .odometerKm(entity.getOdometerKm())
                .created(entity.getCreated())
                .updated(entity.getUpdated())
                .build();
    }
}
