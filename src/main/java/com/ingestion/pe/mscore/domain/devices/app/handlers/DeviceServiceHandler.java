package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static com.ingestion.pe.mscore.domain.devices.app.factory.DeviceWebsocketMessageRefreshFactory.newDeviceUpdate;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.clients.VehicleClient;
import com.ingestion.pe.mscore.clients.cache.store.DeviceCacheStore;
import com.ingestion.pe.mscore.domain.devices.app.resolver.EventResolver;
import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.config.cache.store.RedisPositionStore;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.devices.app.factory.DeviceApplicationEventFactory;
import com.ingestion.pe.mscore.domain.devices.app.manager.ManagerConfigAlert;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceConfigAlertsEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.UserDeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.entity.EventEntity;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceServiceHandler {
        private final DeviceEntityRepository deviceEntityRepository;
        private final UserDeviceEntityRepository userDeviceEntityRepository;
        private final KafkaPublisherService kafkaPublisherService;
        private final HistoricalDeviceEntityRepository historicalDeviceEntityRepository;
        private final DeviceConfigAlertsEntityRepository deviceConfigAlertsEntityRepository;
        // private final EventCreateBridgeService eventCreateBridgeService;
        private final ManagerConfigAlert managerConfigAlert;
        private final VehicleClient vehicleClient;
        private final DeviceCacheStore deviceCacheStore;
        private final com.ingestion.pe.mscore.applications.tracking.TrackingProcessorService trackingProcessorService;
        private final RedisPositionStore redisPositionStore;
        private final PositionMonitoringAsyncDispatcher positionMonitoringAsyncDispatcher;
        private final com.ingestion.pe.mscore.domain.atu.app.dispatcher.AtuTransmissionAsyncDispatcher atuTransmissionAsyncDispatcher;
        private final EventEntityRepository eventEntityRepository;
        private final EventResolver eventResolver;

        @Transactional
        public void handleDeviceEvent(Position position) {
                Optional<DeviceEntity> deviceOpt = deviceEntityRepository.findByImei(position.getImei());

                if (deviceOpt.isPresent()) {
                        DeviceEntity device = deviceOpt.get();
                        try {
                                log.info("[DEBUG] Procesando Dispositivo ID: {}, IMEI: {}",
                                                device.getId(), device.getImei());
                                log.info("[DEBUG] Datos ANTES de update: LastData={}, Speed={}, Lat={}, Lon={}",
                                                device.getLastDataReceived(),
                                                device.getSpeedInKmh(), device.getLatitude(),
                                                device.getLongitude());

                                Long company = Optional.ofNullable(device.getCompany())
                                                .orElseGet(() -> {
                                                        log.warn("El dispositivo con IMEI: {} no tiene una empresa asignada",
                                                                        device.getImei());
                                                        return 0L;
                                                });

                                log.debug("Sensor data traido desde la db desde el service handler {}",
                                                device.getSensorsData());
                                device.handlerPosition(position);
                                log.debug("Sensor data procesado desde el service handler {}",
                                                device.getSensorsData());

                                Set<UUID> usersNotSendPositions = userDeviceEntityRepository
                                                .findAllByDeviceReturnUuids(device);

                                HistoricalDeviceEntity historicalDeviceEntity = HistoricalDeviceEntity
                                                .map(position, device);

                                var historicalSave = historicalDeviceEntityRepository.save(historicalDeviceEntity);

                                device.setLastHistoricalDevice(historicalSave.getId());

                                Map<String, Object> attributes = device.getSensorOnTime();

                                deviceEntityRepository.save(device);
                                log.info("[DEBUG] Dispositivo GUARDADO. Datos AHORA: LastData={}, Speed={}, Lat={}, Lon={}",
                                                device.getLastDataReceived(),
                                                device.getSpeedInKmh(),
                                                device.getLatitude(),
                                                device.getLongitude());

                                trackingProcessorService.processPositionForTracking(position, company);

                                redisPositionStore.savePosition(position, company);

                                Set<DeviceConfigAlertsEntity> configAlertsEntities = managerConfigAlert
                                                .executeConfigAlertRules(device.getId(), attributes);

                                markConfigResolved(device, configAlertsEntities, attributes);
                                markConfigNotResolved(device, configAlertsEntities, usersNotSendPositions, company);

                                deviceConfigAlertsEntityRepository.saveAll(configAlertsEntities);

                                sendNewPosition(device, historicalSave, company);

                                // eventCreateBridgeService.createEvent(
                                // DevicePositionEventCreate.builder()
                                // .deviceId(device.getId())
                                // .deviceTime(position.getDeviceTime() != null
                                // ? position.getDeviceTime().toInstant()
                                // : Instant.now())
                                // .imei(device.getImei())
                                // .sensors(device.getSensor())
                                // .sensorData(sensorData)
                                // .position(position)
                                // .build());
                                saveInCache(device);

                                positionMonitoringAsyncDispatcher.dispatchAsync(
                                                device.getId(),
                                                position.getLatitude(),
                                                position.getLongitude(),
                                                position.getSpeedInKm(),
                                                position.getDeviceTime() != null
                                                                ? position.getDeviceTime().toInstant()
                                                                : Instant.now());

                                atuTransmissionAsyncDispatcher.dispatchAsync(position, device.getId(), company);

                        } catch (Exception e) {
                                log.error("Error procesando evento de dispositivo: {}", e.getMessage(), e);
                                throw e;
                        }
                } else {
                        log.warn("[DEBUG] No se encontró el dispositivo con IMEI: {}", position.getImei());
                }
        }

        private void saveInCache(DeviceEntity device) {
                try {
                        deviceCacheStore.save(device);
                } catch (Exception e) {
                        log.error("Error saving device in cache: {}", e.getMessage(), e);
                }
        }

        /**
         * Marca las configuraciones de alerta que han sido desactivadas y crea los
         * eventos de resolución
         * correspondientes.
         *
         * @param configAlertsEntities Entidades de configuración de alertas
         * @param attributes           Atributos del dispositivo
         */
        private void markConfigResolved(
                        DeviceEntity device,
                        Set<DeviceConfigAlertsEntity> configAlertsEntities, Map<String, Object> attributes) {
                configAlertsEntities.stream()
                                .filter(deactivatedAlert -> !deactivatedAlert.isActive())
                                .forEach(
                                                deactivatedAlert -> {
                                                        String resolutionNotes = "Configuracion de alerta con titulo ["
                                                                        + deactivatedAlert.getConfigAlerts().getTitle()
                                                                        + "] Ya no se encuentra activa";
                                                        if (deactivatedAlert.getEventId() != null) {
                                                                eventEntityRepository
                                                                                .findTopByAggregateIdAndEventTypeAndResolvedFalseOrderByOccurredAtDesc(
                                                                                                device.getId().toString(),
                                                                                                deactivatedAlert.getConfigAlerts()
                                                                                                                .getTitle())
                                                                                .ifPresent(existingEvent -> {
                                                                                        existingEvent.markIsResolved(
                                                                                                        resolutionNotes,
                                                                                                        attributes);
                                                                                        eventEntityRepository.save(
                                                                                                        existingEvent);
                                                                                        eventResolver.resolveEvent(
                                                                                                        existingEvent);
                                                                                });
                                                        }

                                                        deactivatedAlert.markAsResolved();
                                                });
        }

        /**
         * @param device         Dispositivo
         * @param historicalSave Histórico de dispositivo guardado
         * @param company        Empresa ala cual se debe notificar
         */
        protected void sendNewPosition(
                        DeviceEntity device, HistoricalDeviceEntity historicalSave, Long company) {
                WebsocketMessage websocketMessage = newDeviceUpdate(company, device, historicalSave);
                kafkaPublisherService.publishWebsocketMessage(websocketMessage);
        }

        /**
         * Marca las configuraciones de alerta que no se han resuelto y crea los eventos
         * correspondientes.
         *
         * @param device                Dispositivo
         * @param configAlertsEntities  Entidades de configuración de alertas
         * @param usersNotSendPositions Usuarios que no deben recibir notificaciones
         * @param companyId             ID de la empresa
         */
        private void markConfigNotResolved(
                        DeviceEntity device,
                        Set<DeviceConfigAlertsEntity> configAlertsEntities,
                        Set<UUID> usersNotSendPositions,
                        Long companyId) {
                List<VehicleResponse> vehicleAsociateForDevice = configAlertsEntities.stream()
                                .anyMatch(DeviceConfigAlertsEntity::isActive)
                                                ? vehicleClient.getVehiclesByIds(List.of(device.getId()))
                                                : List.of();

                configAlertsEntities.stream()
                                .filter(DeviceConfigAlertsEntity::isActive)
                                .forEach(
                                                triggeredAlertsFor -> {
                                                        log.info(
                                                                        "Alerta de configuración activada para el dispositivo IMEI: {}. Alerta: {}",
                                                                        device.getImei(),
                                                                        triggeredAlertsFor.getConfigAlerts()
                                                                                        .getTitle());
                                                        var event = DeviceApplicationEventFactory
                                                                        .newConfigAlertNotResolved(
                                                                                        device,
                                                                                        usersNotSendPositions,
                                                                                        companyId,
                                                                                        triggeredAlertsFor,
                                                                                        vehicleAsociateForDevice);

                                                        EventEntity eventEntity = EventEntity.map(event);
                                                        eventEntity = eventEntityRepository.save(eventEntity);

                                                        triggeredAlertsFor.setEventId(
                                                                        UUID.fromString(eventEntity.getEventId()));
                                                        triggeredAlertsFor.setSendEventAt(Instant.now());

                                                        eventResolver.resolveEvent(eventEntity);
                                                });
        }
}
