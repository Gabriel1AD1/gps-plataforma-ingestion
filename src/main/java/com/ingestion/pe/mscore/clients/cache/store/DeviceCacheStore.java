package com.ingestion.pe.mscore.clients.cache.store;

import com.ingestion.pe.mscore.clients.models.DeviceResponse;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceCacheStore {

    private static final long TTL_SECONDS = 600;

    private final CacheDao<DeviceResponse> cacheDao;

    /* ================= KEYS ================= */

    private String key(Long companyId, Long deviceId) {
        return "device:" + companyId + ":" + deviceId;
    }

    private String keyById(Long deviceId) {
        return "device:id:" + deviceId;
    }

    private String keyByImei(String imei) {
        return "device:imei:" + imei;
    }

    /* ================= GET ================= */

    public Optional<DeviceResponse> get(Long companyId, Long deviceId) {
        return cacheDao.get(key(companyId, deviceId), DeviceResponse.class);
    }

    public Optional<DeviceResponse> getById(Long deviceId) {
        return cacheDao.get(keyById(deviceId), DeviceResponse.class);
    }

    public Optional<DeviceResponse> getByImei(String imei) {
        return cacheDao.get(keyByImei(imei), DeviceResponse.class);
    }

    /* ================= SAVE ================= */

    public void save(DeviceEntity entity) {
        DeviceResponse response = toResponse(entity);

        cacheDao.save(key(entity.getCompany(), entity.getId()), response, TTL_SECONDS);
        cacheDao.save(keyById(entity.getId()), response, TTL_SECONDS);
        cacheDao.save(keyByImei(entity.getImei()), response, TTL_SECONDS);
    }

    /* ================= DELETE ================= */

    public void delete(DeviceEntity entity) {
        cacheDao.delete(key(entity.getCompany(), entity.getId()));
        cacheDao.delete(keyById(entity.getId()));
        cacheDao.delete(keyByImei(entity.getImei()));
    }

    /* ================= MAPPER ================= */

    private DeviceResponse toResponse(DeviceEntity entity) {
        return new DeviceResponse(
                entity.getId(),
                entity.getImei(),
                entity.getSerialNumber(),
                entity.getPassword(),
                entity.getModel(),
                entity.getCompany(),
                entity.getSensor(),
                entity.getSensorRaw(),
                entity.getDataHistory(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getAltitude(),
                entity.getSpeedInKmh(),
                entity.getSensorDataMap());
    }
}
