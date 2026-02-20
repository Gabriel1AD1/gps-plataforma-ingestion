package com.ingestion.pe.mscore.domain.devices.app.manager;

import org.springframework.stereotype.Component;

@Component
public class AlertRuntimeStateRepository {
    public AlertRuntimeState getOrCreate(Long deviceId, Long configId) {
        return new AlertRuntimeState();
    }

    public void save(Long deviceId, Long configId, AlertRuntimeState state) {

    }
}
