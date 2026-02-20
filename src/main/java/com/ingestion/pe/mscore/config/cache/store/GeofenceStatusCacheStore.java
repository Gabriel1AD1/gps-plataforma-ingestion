package com.ingestion.pe.mscore.config.cache.store;

import com.ingestion.pe.mscore.applications.tracking.enums.GeofenceStatus;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeofenceStatusCacheStore {

    private static final long TTL_SECONDS = 600; // 10 min

    private final CacheDao<GeofenceStatus> cacheDao;

    private String key(Long companyId, String imei, Long geofenceId) {
        return "geofence-status:" + companyId + ":" + imei + ":" + geofenceId;
    }

    public Optional<GeofenceStatus> get(Long companyId, String imei, Long geofenceId) {
        return cacheDao.get(key(companyId, imei, geofenceId), GeofenceStatus.class);
    }

    public void save(Long companyId, String imei, Long geofenceId, GeofenceStatus status) {
        cacheDao.save(key(companyId, imei, geofenceId), status, TTL_SECONDS);
    }

    public void delete(Long companyId, String imei, Long geofenceId) {
        cacheDao.delete(key(companyId, imei, geofenceId));
    }
}
