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

    public Optional<RouteConfigResponse> getRouteConfig(Long routeId) {
        return routeConfigCacheDao.get("route:config:" + routeId, RouteConfigResponse.class);
    }

    // Trip activo individual
    public Optional<TripActiveResponse> getActiveTrip(Long tripId) {
        return tripActiveCacheDao.get("trip:active:" + tripId, TripActiveResponse.class);
    }

    // TripId activo para un vehículo específico
    public Optional<Long> getActiveTripIdForVehicle(Long vehicleId) {
        return tripIdsCacheDao
                .get("trip:active:vehicle:" + vehicleId, Long[].class)
                .map(arr -> arr.length > 0 ? arr[0] : null);
    }

    // Lista de tripIds activos para una ruta
    public List<Long> getActiveTripsForRoute(Long routeId) {
        return tripIdsCacheDao
                .get("trip:active:route:" + routeId, Long[].class)
                .map(Arrays::asList)
                .orElse(List.of());
    }

    public Optional<Long> getVehicleIdByDeviceId(Long deviceId) {
        return vehicleCacheDao
                .get("vehicle:deviceId:" + deviceId, VehicleResponse.class)
                .map(VehicleResponse::getId);
    }

    public Optional<String> getVehiclePlate(Long vehicleId) {
        return vehicleCacheDao
                .get("vehicle:" + vehicleId, VehicleResponse.class)
                .map(VehicleResponse::getLicensePlate);
    }

    public List<Long> getKnownRouteIds() {
        return tripIdsCacheDao
                .get("route:active:ids", Long[].class)
                .map(Arrays::asList)
                .orElse(List.of());
    }
}
