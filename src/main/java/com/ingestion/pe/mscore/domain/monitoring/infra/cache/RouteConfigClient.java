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
            TripActiveResponse response = cached.get();
            if (response.getId() != null && response.getId() == -1L) {
                log.debug("Caché negativo detectado para Trip {}. Saltando Fallback a DB.", tripId);
                return Optional.empty();
            }
            return cached;
        }

        log.warn("[REDIS] Trip {} no encontrado en cache. Intentando fallback a DB.", tripId);
        return tripReadEntityRepository.findById(tripId)
                .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .map(entity -> {
                    TripActiveResponse response = mapToTripActiveResponse(entity);
                    tripActiveCacheDao.save("trip:active:" + tripId, response, 3600);
                    return response;
                })
                .or(() -> {
                    log.debug("No se encontró Trip {} activo en DB. Guardando marcador negativo.", tripId);
                    TripActiveResponse negative = TripActiveResponse.builder().id(-1L).build();
                    tripActiveCacheDao.save("trip:active:" + tripId, negative, 3600);
                    return Optional.empty();
                });
    }

    // TripId activo para un vehículo especifico con fallback a DB
    public Optional<Long> getActiveTripIdForVehicle(Long vehicleId) {
        Optional<Long[]> cachedArr = tripIdsCacheDao.get("trip:active:vehicle:" + vehicleId, Long[].class);
        
        if (cachedArr.isPresent()) {
            Long[] arr = cachedArr.get();
            if (arr.length > 0 && arr[0] != -1L) {
                return Optional.of(arr[0]);
            } else if (arr.length > 0 && arr[0] == -1L) {
                log.debug("Caché negativo ([-1L]) detectado para Vehiculo {}. Saltando Fallback a DB.", vehicleId);
                return Optional.empty(); //  empty
            }
        }

        log.warn("[REDIS] TripId para Vehiculo {} no encontrado en cache. Fallback a DB.", vehicleId);
        Optional<com.ingestion.pe.mscore.domain.dispatch.core.entity.TripReadEntity> tripOpt = tripReadEntityRepository.findFirstByVehicleIdAndStatus(vehicleId, "ACTIVE");
        
        if (tripOpt.isPresent()) {
            Long tId = tripOpt.get().getId();
            tripIdsCacheDao.save("trip:active:vehicle:" + vehicleId, new Long[] { tId }, 3600);
            return Optional.of(tId);
        } else {
            tripIdsCacheDao.save("trip:active:vehicle:" + vehicleId, new Long[] { -1L }, 3600);
            return Optional.empty();
        }
    }

    // Lista de tripIds activos para una ruta con fallback a DB
    public void evictActiveTripIdForVehicle(Long vehicleId) {
        log.warn("Invalidando caché de TripId para Vehiculo {}", vehicleId);
        tripIdsCacheDao.delete("trip:active:vehicle:" + vehicleId);
    }

    public List<Long> getActiveTripsForRoute(Long routeId) {
        Optional<Long[]> cachedArr = tripIdsCacheDao.get("trip:active:route:" + routeId, Long[].class);

        if (cachedArr.isPresent()) {
            Long[] arr = cachedArr.get();
            if (arr.length > 0 && arr[0] == -1L) {
                log.debug("Caché negativo ([-1L]) detectado para Ruta {}. Saltando Fallback a DB.", routeId);
                return java.util.Collections.emptyList(); // Cached as empty
            }
            return java.util.Arrays.asList(arr);
        }

        log.warn("[REDIS] Trips para Ruta {} no encontrados en cache. Fallback a DB.", routeId);
        List<Long> ids = tripReadEntityRepository.findAllByRouteIdAndStatus(routeId, "ACTIVE")
                .stream()
                .map(com.ingestion.pe.mscore.domain.dispatch.core.entity.TripReadEntity::getId)
                .toList();
        
        tripIdsCacheDao.save("trip:active:route:" + routeId, ids.isEmpty() ? new Long[]{-1L} : ids.toArray(new Long[0]), 3600);
        
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
                .driverFullName(entity.getDriverFullName())
                .driverDocumentNumber(entity.getDriverDocumentNumber())
                .atuRouteCode(entity.getAtuRouteCode())
                .build();
    }

    public Optional<Long> getVehicleIdByDeviceId(Long deviceId) {
        String key = "vehicle:deviceId:" + deviceId;
        log.info("[REDIS] Buscando Vehículo para DeviceID={} con clave {}", deviceId, key);
        Optional<VehicleResponse> cached = vehicleCacheDao.get(key, VehicleResponse.class);
        
        if (cached.isPresent()) {
            VehicleResponse response = cached.get();
            if (response.getId() != null && response.getId() == -1L) {
                log.debug("Caché negativo detected para DeviceID={} (Sin vehículo). Saltando Fallback a DB.", deviceId);
                return Optional.empty();
            }
            log.info("[REDIS] Vehículo encontrado en cache: ID={}", response.getId());
            return Optional.of(response.getId());
        }
        
        log.warn("[REDIS] No se encontrado en cache. Intentando fallback a BASE DE DATOS para DeviceID={}", deviceId);
        return vehicleReadEntityRepository.findByDeviceId(deviceId)
                .map(entity -> {
                    log.info("[DB-FALLBACK] Vehículo encontrado en DB: ID={}", entity.getId());
                    VehicleResponse response = mapToVehicleResponse(entity);
                    vehicleCacheDao.save("vehicle:id:" + entity.getId(), response, 3600);
                    vehicleCacheDao.save("vehicle:deviceId:" + deviceId, response, 3600);
                    return entity.getId();
                })
                .or(() -> {
                    log.debug("No se encontró vehículo para DeviceID={} en DB. Guardando marcador negativo.", deviceId);
                    VehicleResponse negative = VehicleResponse.builder().id(-1L).build();
                    vehicleCacheDao.save("vehicle:deviceId:" + deviceId, negative, 3600);
                    return Optional.empty();
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
                })
                .or(() -> {
                    log.debug("No se encontró vehículo {} en DB. Guardando marcador negativo.", vehicleId);
                    VehicleResponse negative = VehicleResponse.builder().id(-1L).licensePlate("N/A").build();
                    vehicleCacheDao.save("vehicle:id:" + vehicleId, negative, 3600);
                    return Optional.empty();
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
        
        tripIdsCacheDao.save("route:active:ids", ids.isEmpty() ? new Long[]{-1L} : ids.toArray(new Long[0]), 3600);
        
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
