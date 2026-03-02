package com.ingestion.pe.mscore.config.cache.store;

import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPositionStore {

    private static final long TTL_SECONDS = 300; // 5 minutes

    private final CacheDao<Position> cacheDao;

    public void savePosition(Position position, Long companyId) {
        if (position == null || companyId == null || position.getImei() == null) {
            log.warn("Posición o companyId no validos: no se peude guardar en Redis");
            return;
        }

        String key = "position:" + companyId + ":" + position.getImei();
        try {
            cacheDao.save(key, position, TTL_SECONDS);
            log.debug("Posición en vivo guardada en Redis para IMEI: {} con TTL {}s", position.getImei(), TTL_SECONDS);
        } catch (Exception e) {
            log.error("Falló al guardar la posición en vivo en Redis para IMEI: {}", position.getImei(), e);
        }
    }
}
