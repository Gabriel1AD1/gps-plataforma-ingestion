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
    private final com.ingestion.pe.mscore.domain.atu.core.repo.AtuTokenReadEntityRepository atuTokenReadEntityRepository;
    private static final String KEY_PREFIX = "atu:company:token:";

    @Override
    public Optional<AtuTokenCache> getAtuConfig(Long companyId) {
        if (companyId == null) return Optional.empty();
        
        String key = KEY_PREFIX + companyId;
        try {
            Optional<AtuTokenCache> cached = cacheDao.get(key, AtuTokenCache.class);
            if (cached.isPresent()) {
                return cached;
            }
            
            log.warn("[REDIS] Token ATU para companyId={} no encontrado en cache. Fallback a DB.", companyId);
            return atuTokenReadEntityRepository.findByCompanyId(companyId)
                    .map(entity -> {
                        AtuTokenCache cache = AtuTokenCache.builder()
                                .token(entity.getToken())
                                .endpoint(entity.getEndpoint())
                                .build();
                        cacheDao.save(key, cache, 3600);
                        return cache;
                    });
            
        } catch (Exception e) {
            log.error("Error leyendo config ATU para companyId={} (Fallback intentado): {}", companyId, e.getMessage());
            return Optional.empty();
        }
    }
}
