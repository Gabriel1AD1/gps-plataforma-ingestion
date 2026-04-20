package com.ingestion.pe.mscore.domain.devices.app.handlers;

import com.ingestion.pe.mscore.clients.VehicleClient;
import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.domain.devices.app.factory.DeviceApplicationEventFactory;
import com.ingestion.pe.mscore.domain.devices.app.manager.ManagerConfigAlert;
import com.ingestion.pe.mscore.domain.devices.app.resolver.EventResolver;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.EventEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceConfigAlertsEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.UserDeviceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertsAsyncDispatcher {

    private final ManagerConfigAlert managerConfigAlert;
    private final DeviceConfigAlertsEntityRepository deviceConfigAlertsEntityRepository;
    private final EventEntityRepository eventEntityRepository;
    private final EventResolver eventResolver;
    private final VehicleClient vehicleClient;
    private final DeviceEntityRepository deviceEntityRepository;
    private final UserDeviceEntityRepository userDeviceEntityRepository;

    @Async("taskExecutor")
    public void dispatchAsync(Long deviceId, Map<String, Object> attributes, Long companyId) {
        log.debug("AlertsAsyncDispatcher: Iniciando evaluación asíncrona - deviceId={}", deviceId);
        try {
            Set<DeviceConfigAlertsEntity> alerts = 
                    managerConfigAlert.executeConfigAlertRules(deviceId, attributes);
            
            if (alerts.isEmpty()) return;

            deviceEntityRepository.findById(deviceId).ifPresent(device -> {
                
                Set<UUID> usersExcluded = userDeviceEntityRepository.findAllByDeviceReturnUuids(device);
                
                markConfigResolved(device, alerts, attributes);
                
                markConfigNotResolved(device, alerts, usersExcluded, companyId);
            
                deviceConfigAlertsEntityRepository.saveAll(alerts);
            });

        } catch (Exception e) {
            log.error("AlertsAsyncDispatcher error: deviceId={} :: {}", deviceId, e.getMessage());
        }
    }

    private void markConfigResolved(DeviceEntity device, 
                                    Set<DeviceConfigAlertsEntity> alerts, 
                                    Map<String, Object> attributes) {
        alerts.stream()
                .filter(a -> !a.isActive())
                .forEach(a -> {
                    String notes = "Configuracion de alerta [" + a.getConfigAlerts().getTitle() + "] resuelta";
                    if (a.getEventId() != null) {
                        eventEntityRepository
                                .findTopByAggregateIdAndEventTypeAndResolvedFalseOrderByOccurredAtDesc(
                                        device.getId().toString(), a.getConfigAlerts().getTitle())
                                .ifPresent(event -> {
                                    event.markIsResolved(notes, attributes);
                                    eventEntityRepository.save(event);
                                    eventResolver.resolveEvent(event);
                                });
                    }
                    a.markAsResolved();
                });
    }

    private void markConfigNotResolved(DeviceEntity device, 
                                       Set<DeviceConfigAlertsEntity> alerts,
                                       Set<UUID> usersExcluded, 
                                       Long companyId) {
        
        List<VehicleResponse> vehicles = alerts.stream().anyMatch(a -> a.isActive())
                ? vehicleClient.getVehiclesByIds(List.of(device.getId()))
                : List.of();

        alerts.stream()
                .filter(a -> a.isActive())
                .forEach(a -> {
                    log.info("AlertsAsyncDispatcher: Alerta activada IMEI={} Título={}", 
                            device.getImei(), a.getConfigAlerts().getTitle());
                    
                    var event = DeviceApplicationEventFactory.newConfigAlertNotResolved(
                            device, usersExcluded, companyId, a, vehicles);

                    EventEntity entity = EventEntity.map(event);
                    entity = eventEntityRepository.save(entity);

                    a.setEventId(entity.getEventId());
                    a.setSendEventAt(Instant.now());

                    eventResolver.resolveEvent(entity);
                });
    }
}
