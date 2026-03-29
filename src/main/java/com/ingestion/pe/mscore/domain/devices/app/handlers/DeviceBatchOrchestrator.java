package com.ingestion.pe.mscore.domain.devices.app.handlers;

import com.ingestion.pe.mscore.clients.VehicleClient;
import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.devices.app.factory.DeviceApplicationEventFactory;
import com.ingestion.pe.mscore.domain.devices.app.manager.ManagerConfigAlert;
import com.ingestion.pe.mscore.domain.devices.app.resolver.EventResolver;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.EventEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.UserDeviceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceBatchOrchestrator {

    private final DeviceEntityRepository deviceEntityRepository;
    private final UserDeviceEntityRepository userDeviceEntityRepository;
    private final ManagerConfigAlert managerConfigAlert;
    private final VehicleClient vehicleClient;
    private final EventEntityRepository eventEntityRepository;
    private final EventResolver eventResolver;
    private final DeviceBatchPersistenceService persistenceService;

    public void processBatch(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        log.info("Procesando batch de positions: size={}", messages.size());

        List<Position> validPositions = messages.stream()
                .map(this::parsePosition)
                .filter(Objects::nonNull)
                .toList();

        if (validPositions.isEmpty()) return;

        Map<String, List<Position>> groupedByImei = validPositions.stream()
                .collect(Collectors.groupingBy(Position::getImei));

        Set<String> imeis = groupedByImei.keySet();
        Map<String, DeviceEntity> deviceMap = deviceEntityRepository.findByImeiIn(imeis).stream()
                .collect(Collectors.toMap(DeviceEntity::getImei, d -> d));

        Map<String, Set<UUID>> exclusionMap = loadExclusionsInBulk(imeis);

        List<ProcessedResult> results = new ArrayList<>();

        for (Map.Entry<String, List<Position>> entry : groupedByImei.entrySet()) {
            String imei = entry.getKey();
            DeviceEntity device = deviceMap.get(imei);
            if (device == null) {
                log.warn("Device no encontrado para IMEI: {}", imei);
                continue;
            }

            Set<UUID> exclusions = exclusionMap.getOrDefault(imei, Collections.emptySet());
            List<Position> sortedPositions = entry.getValue();
            sortedPositions.sort(Comparator.comparing(p -> 
                p.getDeviceTime() != null ? p.getDeviceTime() : p.getServerTime()));

            for (Position position : sortedPositions) {
                try {
                    device.handlerPosition(position);
                    HistoricalDeviceEntity historical = HistoricalDeviceEntity.map(position, device);
                    
                    Map<String, Object> attributes = device.getSensorOnTime();
                    Set<DeviceConfigAlertsEntity> alerts = managerConfigAlert.executeConfigAlertRules(device.getId(), attributes);
                    
                    List<EventEntity> triggeredEvents = processAlertRealtime(device, alerts, attributes, exclusions);

                    results.add(new ProcessedResult(device, position, historical, alerts, triggeredEvents));
                } catch (Exception e) {
                    log.error("Proceso fallido para IMEI {}: {}", imei, e.getMessage());
                }
            }
        }

        if (!results.isEmpty()) {
            persistenceService.saveBatch(results);
        }
    }

    private List<EventEntity> processAlertRealtime(DeviceEntity device, Set<DeviceConfigAlertsEntity> alerts, 
                                                  Map<String, Object> attributes, Set<UUID> exclusions) {
        List<EventEntity> events = new ArrayList<>();
        
        alerts.stream()
            .filter(alert -> !alert.isActive())
            .forEach(alert -> {
                if (alert.getEventId() != null) {
                    eventEntityRepository.findTopByAggregateIdAndEventTypeAndResolvedFalseOrderByOccurredAtDesc(
                        device.getId().toString(), alert.getConfigAlerts().getTitle())
                    .ifPresent(event -> {
                        event.markIsResolved("Resuelto por lógica de batch", attributes);
                        events.add(event); 
                    });
                }
                alert.markAsResolved();
            });

        boolean hasActive = alerts.stream().anyMatch(DeviceConfigAlertsEntity::isActive);
        List<VehicleResponse> vehicles = hasActive ? vehicleClient.getVehiclesByIds(List.of(device.getId())) : List.of();

        alerts.stream()
            .filter(DeviceConfigAlertsEntity::isActive)
            .forEach(alert -> {
                var eventDto = DeviceApplicationEventFactory.newConfigAlertNotResolved(
                    device, exclusions, device.getCompany(), alert, vehicles);
                events.add(EventEntity.map(eventDto));
            });

        return events;
    }

    private Map<String, Set<UUID>> loadExclusionsInBulk(Set<String> imeis) {
        Map<String, Set<UUID>> map = new HashMap<>();
        List<Object[]> rows = userDeviceEntityRepository.findExcludedUuidsByImeiIn(imeis);
        for (Object[] row : rows) {
            String imei = (String) row[0];
            UUID uuid = (UUID) row[1];
            map.computeIfAbsent(imei, k -> new HashSet<>()).add(uuid);
        }
        return map;
    }

    private Position parsePosition(String message) {
        try {
            return Position.fromJson(message);
        } catch (Exception e) {
            log.error("JSON inválido position: {}", message);
            return null;
        }
    }

    @lombok.Value
    public static class ProcessedResult {
        DeviceEntity device;
        Position position;
        HistoricalDeviceEntity historical;
        Set<DeviceConfigAlertsEntity> alerts;
        List<EventEntity> events;

        public Long getCompany() {
            return device != null ? device.getCompany() : 0L;
        }
    }
}
