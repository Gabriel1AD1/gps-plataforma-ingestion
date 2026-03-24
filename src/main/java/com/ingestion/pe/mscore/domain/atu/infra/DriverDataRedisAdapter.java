package com.ingestion.pe.mscore.domain.atu.infra;

import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.domain.atu.model.DriverCacheData;
import com.ingestion.pe.mscore.domain.atu.app.port.DriverDataPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverDataRedisAdapter implements DriverDataPort {

    private final CacheDao<DriverCacheData> cacheDao;
    private static final String KEY_PREFIX = "driver:data:";

    @Override
    public Optional<String> getDriverDocumentNumber(Long driverId) {
        if (driverId == null) return Optional.empty();

        String key = KEY_PREFIX + driverId;
        try {
            return cacheDao.get(key, DriverCacheData.class)
                    .map(DriverCacheData::getDocumentNumber)
                    .filter(doi -> !doi.isBlank());
        } catch (Exception e) {
            log.warn("Error leyendo DNI de conductor en Redis para driverId={}: {}", driverId, e.getMessage());
            return Optional.empty();
        }
    }
}
