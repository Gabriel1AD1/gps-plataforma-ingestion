package com.ingestion.pe.mscore.domain.devices.app.handlers;
import com.ingestion.pe.mscore.domain.monitoring.app.handler.PositionMonitoringHook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionMonitoringAsyncDispatcher {

    private final PositionMonitoringHook positionMonitoringHook;

    @Async
    public void dispatchAsync(Long deviceId, double lat, double lon, double speedKmh, Instant time) {
        try {
            positionMonitoringHook.onPositionReceived(deviceId, lat, lon, speedKmh, time);
        } catch (Exception e) {
            log.error("Error async en PositionMonitoringHook para deviceId={}: {}", deviceId, e.getMessage());
        }
    }
}
