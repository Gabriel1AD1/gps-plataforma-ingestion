package com.ingestion.pe.mscore.domain.atu.infra;

import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.domain.atu.model.AtuTokenCache;
import com.ingestion.pe.mscore.domain.atu.app.port.AtuConfigPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AtuConfigRedisAdapter implements AtuConfigPort {

    private final CacheDao<AtuTokenCache> cacheDao;
    private static final String KEY_PREFIX = "atu:company:token:";

    @Override
    public Optional<AtuTokenCache> getAtuConfig(Long companyId) {
        if (companyId == null) return Optional.empty();
        
        String key = KEY_PREFIX + companyId;
        try {
            return cacheDao.get(key, AtuTokenCache.class);
        } catch (Exception e) {
            log.warn("Error leyendo configuración ATU para companyId={} desde Redis: {}", companyId, e.getMessage());
            return Optional.empty();
        }
    }
}
