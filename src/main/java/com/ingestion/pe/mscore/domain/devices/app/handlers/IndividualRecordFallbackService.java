package com.ingestion.pe.mscore.domain.devices.app.handlers;

import com.ingestion.pe.mscore.domain.devices.app.handlers.DeviceBatchOrchestrator.ProcessedResult;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceConfigAlertsEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndividualRecordFallbackService {

    private final DeviceEntityRepository deviceEntityRepository;
    private final HistoricalDeviceEntityRepository historicalDeviceEntityRepository;
    private final DeviceConfigAlertsEntityRepository deviceConfigAlertsEntityRepository;
    private final EventEntityRepository eventEntityRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleRecord(ProcessedResult result) {
        String imei = result.getDevice().getImei();
        try {
            var historical = historicalDeviceEntityRepository.save(result.getHistorical());
            
            DeviceEntity freshDevice = deviceEntityRepository.findById(result.getDevice().getId())
                    .orElse(result.getDevice());

            applyPositionToFreshDevice(freshDevice, result.getDevice(), historical.getId());
            
            deviceEntityRepository.save(freshDevice);

            if (result.getAlerts() != null && !result.getAlerts().isEmpty()) {
                deviceConfigAlertsEntityRepository.saveAll(result.getAlerts());
            }

            if (result.getEvents() != null && !result.getEvents().isEmpty()) {
                eventEntityRepository.saveAll(result.getEvents());
            }

            log.debug("Fallback exitoso para IMEI: {}. Consistencia garantizada por Fetch-and-Merge.", imei);
        } catch (Exception e) {
            log.error("Fallback fallo para IMEI: {}. Registro descartado. Error: {}", imei, e.getMessage());
            throw e; 
        }
    }

    private void applyPositionToFreshDevice(DeviceEntity target, DeviceEntity source, Long historicalId) {
        target.setLatitude(source.getLatitude());
        target.setLongitude(source.getLongitude());
        target.setOdometerInMeters(source.getOdometerInMeters());
        target.setSpeedInKmh(source.getSpeedInKmh());
        target.setDeviceTime(source.getDeviceTime());
        target.setLastHistoricalDevice(historicalId);
        target.setSensor(source.getSensor());
        target.setSensorRaw(source.getSensorRaw());
        target.setSensorOnTime(source.getSensorOnTime());
        target.setSensorsData(source.getSensorsData());
        target.setLastDataReceived(source.getLastDataReceived());
    }
}
