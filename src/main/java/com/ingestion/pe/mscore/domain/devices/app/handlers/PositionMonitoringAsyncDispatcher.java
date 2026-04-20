package com.ingestion.pe.mscore.domain.devices.app.handlers;

import com.ingestion.pe.mscore.applications.tracking.TrackingProcessorService;
import com.ingestion.pe.mscore.commons.models.Position;
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
    private final TrackingProcessorService trackingProcessorService;

    /**
     * Procesa asíncronamente las tareas de monitoreo y geocercas.
     */
    @Async("taskExecutor")
    public void dispatchAsync(Long deviceId, double lat, double lon, double speedKmh, Instant time, Position position, Long companyId) {
        // Ejecución de Monitoreo (Hook de rutas)
        try {
            positionMonitoringHook.onPositionReceived(deviceId, lat, lon, speedKmh, time, position, companyId);
        } catch (Exception e) {
            log.error("Error async en PositionMonitoringHook para deviceId={}: {}", deviceId, e.getMessage());
        }

        // Geocercas y distancias
        try {
            trackingProcessorService.processPositionForTracking(position, companyId);
        } catch (Exception e) {
            log.error("Error async en TrackingProcessorService para deviceId={}: {}", deviceId, e.getMessage());
        }
    }
}
