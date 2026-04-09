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
    private final com.ingestion.pe.mscore.domain.drivers.core.repo.DriverReadEntityRepository driverReadEntityRepository;
    private static final String KEY_PREFIX = "driver:data:";

    @Override
    public Optional<String> getDriverDocumentNumber(Long driverId) {
        if (driverId == null) return Optional.empty();

        String key = KEY_PREFIX + driverId;
        try {
            Optional<String> cached = cacheDao.get(key, DriverCacheData.class)
                    .map(DriverCacheData::getDocumentNumber)
                    .filter(doi -> !doi.isBlank());
            
            if (cached.isPresent()) {
                return cached;
            }

            log.warn("[REDIS] DNI de conductor para driverId={} no encontrado en cache. Fallback a DB.", driverId);
            return driverReadEntityRepository.findById(driverId)
                    .map(entity -> {
                        DriverCacheData data = DriverCacheData.builder()
                                .documentNumber(entity.getDocumentNumber())
                                .build();
                        cacheDao.save(key, data, 3600);
                        return entity.getDocumentNumber();
                    })
                    .filter(doi -> !doi.isBlank());

        } catch (Exception e) {
            log.error("Error leyendo DNI de conductor para driverId={} (Fallback intentado): {}", driverId, e.getMessage());
            return Optional.empty();
        }
    }
}
