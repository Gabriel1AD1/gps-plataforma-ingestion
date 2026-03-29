package com.ingestion.pe.mscore.domain.devices.app.handlers;

import com.ingestion.pe.mscore.domain.devices.app.handlers.DeviceBatchOrchestrator.ProcessedResult;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.EventEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceConfigAlertsEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceBatchPersistenceService {

    private final DeviceEntityRepository deviceEntityRepository;
    private final HistoricalDeviceEntityRepository historicalDeviceEntityRepository;
    private final DeviceConfigAlertsEntityRepository deviceConfigAlertsEntityRepository;
    private final EventEntityRepository eventEntityRepository;
    private final IndividualRecordFallbackService individualFallbackService;
    private final PostPersistenceOrchestrator postPersistenceOrchestrator;

    public void saveBatch(List<ProcessedResult> results) {
        try {
            this.persistBulk(results);
            log.info("Persistencia batch exitosa de {} registros", results.size());
            postPersistenceOrchestrator.executePostPersistence(results);
        } catch (DataIntegrityViolationException | JpaSystemException e) {
            log.warn("Persistencia batch fallida: [{}]. Cambiando a fallback individual", e.getMessage());
            this.persistIndividually(results);
        } catch (Exception e) {
            log.error("Error inesperado en persistencia batch: {}", e.getMessage(), e);
            this.persistIndividually(results);
        }
    }

    @Transactional
    public void persistBulk(List<ProcessedResult> results) {
        List<HistoricalDeviceEntity> historicals = results.stream()
                .map(ProcessedResult::getHistorical)
                .toList();
        historicalDeviceEntityRepository.saveAll(historicals);

        List<DeviceEntity> devices = results.stream()
                .map(r -> {
                    DeviceEntity d = r.getDevice();
                    d.setLastHistoricalDevice(r.getHistorical().getId());
                    return d;
                })
                .distinct() 
                .toList();
        deviceEntityRepository.saveAll(devices);

        List<DeviceConfigAlertsEntity> alerts = results.stream()
                .flatMap(r -> r.getAlerts().stream())
                .toList();
        if (!alerts.isEmpty()) {
            deviceConfigAlertsEntityRepository.saveAll(alerts);
        }

        List<EventEntity> events = results.stream()
                .flatMap(r -> r.getEvents().stream())
                .toList();
        if (!events.isEmpty()) {
            eventEntityRepository.saveAll(events);
        }
    }

    private void persistIndividually(List<ProcessedResult> results) {
        List<ProcessedResult> successfulOnes = new ArrayList<>();
        for (ProcessedResult result : results) {
            String imei = result.getDevice().getImei();
            try {
                individualFallbackService.processSingleRecord(result);
                successfulOnes.add(result);
            } catch (Exception e) {
                log.warn("Registro para IMEI {} descartado permanentemente en el batch actual debido a falla técnica.", imei);
            }
        }
        
        if (!successfulOnes.isEmpty()) {
            postPersistenceOrchestrator.executePostPersistence(successfulOnes);
        }
    }
}
