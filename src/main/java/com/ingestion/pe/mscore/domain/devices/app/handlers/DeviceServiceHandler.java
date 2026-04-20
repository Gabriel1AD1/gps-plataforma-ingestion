package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static com.ingestion.pe.mscore.domain.devices.app.factory.DeviceWebsocketMessageRefreshFactory.newDeviceUpdate;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.clients.cache.store.DeviceCacheStore;
import com.ingestion.pe.mscore.clients.models.DeviceResponse;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.config.cache.store.RedisPositionStore;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.devices.app.batch.HistoricalBatchSaver;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.models.SensorModel;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceServiceHandler {
    private final DeviceEntityRepository deviceEntityRepository;
    private final KafkaPublisherService kafkaPublisherService;
    private final DeviceCacheStore deviceCacheStore;
    private final RedisPositionStore redisPositionStore;
    private final PositionMonitoringAsyncDispatcher positionMonitoringAsyncDispatcher;

    private final HistoricalBatchSaver historicalBatchSaver;
    private final AlertsAsyncDispatcher alertsAsyncDispatcher;

    public void handleDeviceEvent(Position position) {
        DeviceEntity device = resolveDevice(position.getImei());
        if (device == null) {
            log.warn("DeviceServiceHandler: Dispositivo no registrado - IMEI: {}", position.getImei());
            return;
        }

        try {
            log.info("[DEBUG] Procesando Dispositivo ID: {}, IMEI: {}", device.getId(), device.getImei());
            Long companyId = Optional.ofNullable(device.getCompany()).orElse(0L);
            String traceUuid = position.getCorrelationId();

            device.handlerPosition(position);
   
            saveInCache(device);
            redisPositionStore.savePosition(position, companyId);
            
            sendNewPosition(device, companyId, traceUuid);

            HistoricalDeviceEntity historical = HistoricalDeviceEntity.map(position, device);
            historicalBatchSaver.enqueue(historical);

            Map<String, Object> sensorSnapshot = new HashMap<>(device.getSensorOnTime());
            alertsAsyncDispatcher.dispatchAsync(device.getId(), sensorSnapshot, companyId);

            positionMonitoringAsyncDispatcher.dispatchAsync(
                    device.getId(),
                    position.getLatitude(),
                    position.getLongitude(),
                    position.getSpeedInKm(),
                    position.getDeviceTime() != null ? position.getDeviceTime().toInstant() : Instant.now(),
                    position,
                    companyId);


            log.info("[DEBUG] Dispositivo procesado en Fast-Lane. IMEI: {}", device.getImei());

        } catch (Exception e) {
            log.error("DeviceServiceHandler: Error procesando IMEI {} (ID {}). Detalle: {}", 
                    position.getImei(), device.getId(), e.getMessage(), e);
        }
    }

    private DeviceEntity resolveDevice(String imei) {
        return deviceCacheStore.getByImei(imei)
                .map(this::mapToEntity)
                .orElseGet(() -> {
                    log.info("DeviceServiceHandler: Cache MISS IMEI {}, cargando de DB", imei);
                    return deviceEntityRepository.findByImei(imei)
                            .map(entity -> {
                                deviceCacheStore.save(entity);
                                return entity;
                            }).orElse(null);
                });
    }

    private DeviceEntity mapToEntity(DeviceResponse response) {
        DeviceEntity entity = new DeviceEntity();
        entity.setId(response.getId());
        entity.setImei(response.getImei());
        entity.setCompany(response.getCompanyId());
        entity.setLatitude(response.getLatitude());
        entity.setLongitude(response.getLongitude());
        entity.setSpeedInKmh(response.getSpeedInKmh());
        entity.setAltitude(response.getAltitude());
        entity.setSensor(response.getSensor());
        entity.setSensorRaw(response.getSensorRaw());
        entity.setDataHistory(response.getDataHistory());
        
        if (response.getSensorData() != null) {
            Set<SensorModel> sensors = response.getSensorData().stream()
                    .map(m -> SensorModel.builder()
                            .key((String) m.get("key"))
                            .value((String) m.get("value"))
                            .timestamp(m.get("timestamp") != null ? Instant.parse(m.get("timestamp").toString()) : Instant.now())
                            .lastStateChangeTimestamp(m.get("lastStateChangeTimestamp") != null ? Instant.parse(m.get("lastStateChangeTimestamp").toString()) : Instant.now())
                            .timeInCurrentState(m.get("timeInCurrentState") != null ? Long.valueOf(m.get("timeInCurrentState").toString()) : 0L)
                            .build())
                    .collect(Collectors.toSet());
            entity.setSensorsData(sensors);
            entity.setSensorOnTime(entity.getSensorOnTime(sensors));
        } else {
            entity.setSensorsData(new HashSet<>());
            entity.setSensorOnTime(new HashMap<>());
        }
        
        return entity;
    }

    private void saveInCache(DeviceEntity device) {
        try {
            deviceCacheStore.save(device);
        } catch (Exception e) {
            log.error("DeviceServiceHandler cache sync error: {}", e.getMessage());
        }
    }

    protected void sendNewPosition(DeviceEntity device, Long company, String traceUuid) {
        WebsocketMessage websocketMessage = newDeviceUpdate(company, device, null, traceUuid);
        kafkaPublisherService.publishWebsocketMessage(websocketMessage);
    }
}
