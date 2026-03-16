package com.ingestion.pe.mscore.domain.devices.app.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingestion.pe.mscore.domain.devices.core.entity.ConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * configAlert:device:{deviceId}
 * ConfigAlertCache
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheRepository {

    private final CacheDao<String> cacheDao;
    public static final String CONFIG_ALERT_KEY_PREFIX = "configAlert:device:";

    public List<DeviceConfigAlertsEntity> get(Long deviceId) {
        try {
            String key = CONFIG_ALERT_KEY_PREFIX + deviceId;
            var cached = cacheDao.get(key, String.class);
            if (cached.isEmpty()) {
                log.debug("No hay reglas de alerta en Redis para deviceId={}", deviceId);
                return Collections.emptyList();
            }

            List<Map<String, Object>> entries = JsonUtils.toList(
                cached.get(),
                new TypeReference<List<Map<String, Object>>>() {}
            );
            if (entries == null || entries.isEmpty()) {
                return Collections.emptyList();
            }

            return entries.stream()
                .map(this::toCacheEntity)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error leyendo alertas de Redis para deviceId={}: {}", deviceId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private DeviceConfigAlertsEntity toCacheEntity(Map<String, Object> entry) {
        ConfigAlertsEntity config = new ConfigAlertsEntity();
        config.setId(toLong(entry.get("configAlertId")));
        config.setJexlScript((String) entry.get("jexlScript"));
        config.setTitle((String) entry.get("title"));
        config.setDescription((String) entry.get("description"));
        config.setCountEventForActivate(toLong(entry.get("countEventForActivate")));
        config.setCountEventForDeactivate(toLong(entry.get("countEventForDeactivate")));

        DeviceConfigAlertsEntity dca = new DeviceConfigAlertsEntity();
        dca.setId(toLong(entry.get("deviceConfigAlertId")));
        dca.setConfigAlerts(config);
        return dca;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

