package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static com.ingestion.pe.mscore.domain.devices.app.factory.DeviceWebsocketMessageRefreshFactory.newDeviceUpdate;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.clients.cache.store.DeviceCacheStore;
import com.ingestion.pe.mscore.config.cache.store.RedisPositionStore;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.devices.app.handlers.DeviceBatchOrchestrator.ProcessedResult;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.atu.app.dispatcher.AtuTransmissionAsyncDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostPersistenceOrchestrator {

    private final RedisPositionStore redisPositionStore;
    private final DeviceCacheStore deviceCacheStore;
    private final KafkaPublisherService kafkaPublisherService;
    private final PositionMonitoringAsyncDispatcher positionMonitoringAsyncDispatcher;
    private final AtuTransmissionAsyncDispatcher atuTransmissionAsyncDispatcher;

    public void executePostPersistence(List<ProcessedResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        log.info("Distribuicion Post-Persistencia: {} registros", results.size());

        Map<String, DeviceEntity> finalDeviceStates = results.stream()
                .collect(Collectors.toMap(
                        r -> r.getDevice().getImei(),
                        ProcessedResult::getDevice,
                        (existing, replacement) -> replacement 
                ));

        finalDeviceStates.values().forEach(this::safeSyncDeviceCache);

        Map<String, List<ProcessedResult>> groupedByImei = results.stream()
                .collect(Collectors.groupingBy(r -> r.getDevice().getImei()));

        groupedByImei.forEach((imei, imeiResults) -> {
            try (var ignored = MDC.putCloseable("imei", imei)) {
                distributeImeiBatch(imei, imeiResults);
            }
        });
    }

    private void distributeImeiBatch(String imei, List<ProcessedResult> imeiResults) {
        safeExecute("GeofencesBatch", () -> 
            positionMonitoringAsyncDispatcher.dispatchBatchAsync(imei, imeiResults)
        );

        for (ProcessedResult result : imeiResults) {
            safeIndividualDistribution(result);
        }
    }

    private void safeIndividualDistribution(ProcessedResult result) {
        Position pos = result.getPosition();
        DeviceEntity dev = result.getDevice();
        HistoricalDeviceEntity hist = result.getHistorical();
        Long company = result.getCompany();

        safeExecute("RedisPosition", () -> redisPositionStore.savePosition(pos, company));
        
        safeExecute("WebSocket", () -> {
            WebsocketMessage wsMsg = newDeviceUpdate(company, dev, hist);
            kafkaPublisherService.publishWebsocketMessage(wsMsg);
        });

        safeExecute("ATU", () -> atuTransmissionAsyncDispatcher.dispatchAsync(pos, dev.getId(), company));
    }

    private void safeSyncDeviceCache(DeviceEntity device) {
        safeExecute("DeviceCacheSync", () -> deviceCacheStore.save(device));
    }

    private void safeExecute(String taskName, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("Error en tarea Post-Persistencia [{}] para IMEI {}: {}", 
                     taskName, MDC.get("imei"), e.getMessage());
        }
    }
}
