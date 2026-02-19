package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static com.ingestion.pe.mscore.domain.devices.app.factory.DeviceWebsocketMessageRefreshFactory.newDeviceUpdate;

import com.ingestion.pe.mscore.bridge.pub.models.DevicePositionEventCreate;
import com.ingestion.pe.mscore.bridge.pub.models.ResolvedApplicationEvent;
import com.ingestion.pe.mscore.bridge.pub.service.EventCreateBridgeService;
import com.ingestion.pe.mscore.clients.VehicleClient;
import com.ingestion.pe.mscore.clients.cache.store.DeviceCacheStore;
import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.devices.app.factory.DeviceApplicationEventFactory;
import com.ingestion.pe.mscore.domain.devices.app.manager.ManagerConfigAlert;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.models.SensorModel;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceConfigAlertsEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.UserDeviceEntityRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        private final EventCreateBridgeService eventCreateBridgeService;
        private final ManagerConfigAlert managerConfigAlert;
        private final VehicleClient vehicleClient;
        private final DeviceCacheStore deviceCacheStore;

        /*
         * Manejador de los posiciones de los pocisiones
         */
        @Transactional
        public void handleDeviceEvent(Position position) {
                Optional<DeviceEntity> deviceOpt = deviceEntityRepository.findByImei(position.getImei());

                if (deviceOpt.isPresent()) {
                        DeviceEntity device = deviceOpt.get();
                        try {
                                log.info("🔍 [DEBUG] Procesando Dispositivo ID: {}, IMEI: {}",
                                                device.getId(), device.getImei());
                                log.info("🔍 [DEBUG] Datos ANTES de update: LastData={}, Speed={}, Lat={}, Lon={}",
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
                                log.info("✅ [DEBUG] Dispositivo GUARDADO. Datos AHORA: LastData={}, Speed={}, Lat={}, Lon={}",
                                                device.getLastDataReceived(),
                                                device.getSpeedInKmh(),
                                                device.getLatitude(),
                                                device.getLongitude());

                                // Se aplican las reglas y vuelve a obtener las alertas
                                Set<DeviceConfigAlertsEntity> configAlertsEntities = managerConfigAlert
                                                .executeConfigAlertRules(device.getId(), attributes);

                                // Mutamos las configuraciones de alerta resueltas
                                markConfigResolved(configAlertsEntities, attributes);
                                // Mutamos las configuraciones de alerta no resueltas
                                markConfigNotResolved(device, configAlertsEntities, usersNotSendPositions, company);

                                // Se guardan los cambios en las alertas
                                deviceConfigAlertsEntityRepository.saveAll(configAlertsEntities);

                                sendNewPosition(device, historicalSave, company);
                                Set<Map<String, Object>> sensorData = device.getSensorsData()
                                                .stream()
                                                .map(SensorModel::toMap)
                                                .collect(Collectors.toSet());

                                eventCreateBridgeService.createEvent(
                                                DevicePositionEventCreate.builder()
                                                                .deviceId(device.getId())
                                                                .imei(device.getImei())
                                                                .sensors(device.getSensor())
                                                                .sensorData(sensorData)
                                                                .position(position)
                                                                .build());

                                saveInCache(device);

                        } catch (Exception e) {
                                log.error("Error procesando evento de dispositivo: {}", e.getMessage(), e);
                                throw e; // Relanzar para manejo superior si es necesario
                        }
                } else {
                        log.warn("❌ [DEBUG] No se encontró el dispositivo con IMEI: {}", position.getImei());
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
                        Set<DeviceConfigAlertsEntity> configAlertsEntities, Map<String, Object> attributes) {
                configAlertsEntities.stream()
                                .filter(deactivatedAlert -> !deactivatedAlert.isActive())
                                .forEach(
                                                deactivatedAlert -> {
                                                        String resolutionNotes = "Configuracion de alerta con titulo ["
                                                                        + deactivatedAlert.getConfigAlerts().getTitle()
                                                                        + "] Ya no se encuentra activa";
                                                        eventCreateBridgeService.createEvent(
                                                                        ResolvedApplicationEvent.builder()
                                                                                        .eventId(deactivatedAlert
                                                                                                        .getEventId())
                                                                                        .resolutionNotes(
                                                                                                        resolutionNotes)
                                                                                        .resolution(true)
                                                                                        .resolutionProperties(
                                                                                                        attributes)
                                                                                        .build());
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
                configAlertsEntities.stream()
                                .filter(DeviceConfigAlertsEntity::isActive)
                                .forEach(
                                                triggeredAlertsFor -> {
                                                        List<VehicleResponse> vehicleAsociateForDevice = vehicleClient
                                                                        .getVehiclesByIds(List.of(device.getId()));

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
                                                        triggeredAlertsFor.setSendEventAt(Instant.now());
                                                        eventCreateBridgeService.createEvent(event);
                                                });
        }
}
