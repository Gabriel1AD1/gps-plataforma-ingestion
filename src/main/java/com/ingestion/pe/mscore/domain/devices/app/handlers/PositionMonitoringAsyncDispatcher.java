package com.ingestion.pe.mscore.domain.devices.app.handlers;

import com.ingestion.pe.mscore.applications.tracking.TrackingProcessorService;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.devices.app.handlers.DeviceBatchOrchestrator.ProcessedResult;
import com.ingestion.pe.mscore.domain.monitoring.app.handler.PositionMonitoringHook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionMonitoringAsyncDispatcher {

    private final PositionMonitoringHook positionMonitoringHook;
    private final TrackingProcessorService trackingProcessorService;

    @Async("asyncExecutor")
    public void dispatchBatchAsync(String imei, List<ProcessedResult> results) {
        try (var ignored = MDC.putCloseable("imei", imei)) {
            if (results == null || results.isEmpty()) return;

            Long companyId = results.get(0).getCompany();
            List<Position> positions = results.stream().map(ProcessedResult::getPosition).toList();

            trackingProcessorService.processPositionsBatchForTracking(imei, positions, companyId);

            for (ProcessedResult result : results) {
                Position pos = result.getPosition();
                positionMonitoringHook.onPositionReceived(
                        result.getDevice().getId(),
                        pos.getLatitude(),
                        pos.getLongitude(),
                        pos.getSpeedInKm(),
                        pos.getDeviceTime() != null ? pos.getDeviceTime().toInstant() : Instant.now()
                );
            }
        } catch (Exception e) {
            log.error("Error en despacho batch asincrono para IMEI {}: {}", imei, e.getMessage());
        }
    }

    @Async("asyncExecutor")
    public void dispatchAsync(Long deviceId, double lat, double lon, double speedKmh, Instant time) {
        try {
            positionMonitoringHook.onPositionReceived(deviceId, lat, lon, speedKmh, time);
        } catch (Exception e) {
            log.error("Error async en PositionMonitoringHook para deviceId={}: {}", deviceId, e.getMessage());
        }
    }
}
