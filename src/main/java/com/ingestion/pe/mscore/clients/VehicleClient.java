package com.ingestion.pe.mscore.clients;

import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleClient {

    private final CacheDao<VehicleResponse> cacheDao;

    public List<VehicleResponse> getVehiclesByIds(List<Long> deviceIds) {
        return deviceIds.stream()
                .map(deviceId -> {
                    String key = "vehicle:deviceId:" + deviceId;
                    Optional<VehicleResponse> vehicleOpt = cacheDao.get(key, VehicleResponse.class);
                    if (vehicleOpt.isEmpty()) {
                        log.debug("Vehículo no encontrado en Redis para deviceId={}", deviceId);
                    }
                    return vehicleOpt.orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
