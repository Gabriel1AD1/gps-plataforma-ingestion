package com.ingestion.pe.mscore.domain.devices.app.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingestion.pe.mscore.bridge.pub.models.ApplicationEventCreate;
import com.ingestion.pe.mscore.bridge.pub.models.enums.ApplicationName;
import com.ingestion.pe.mscore.bridge.pub.models.enums.ModuleName;
import com.ingestion.pe.mscore.bridge.pub.models.GeofenceEventDto;
import com.ingestion.pe.mscore.commons.models.enums.EventTypeEnumerated;
import com.ingestion.pe.mscore.domain.auth.core.enums.NotificationTypes;
import com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleGeofenceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GeofenceApplicationEventFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Evento entrada/salida
     *
     * @param eventDto       
     * @param geofenceConfig 
     *                       
     * @return 
     */
    public static ApplicationEventCreate forGeofenceEvent(
            GeofenceEventDto eventDto,
            VehicleGeofenceEntity geofenceConfig) {

        String eventId = UUID.randomUUID().toString();
        String title = buildTitle(eventDto);
        String description = buildDescription(eventDto);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("imei", eventDto.getImei());
        properties.put("geofenceId", eventDto.getGeofenceId());
        properties.put("geofenceName", eventDto.getGeofenceName());
        properties.put("eventType", eventDto.getEventType());
        properties.put("latitude", eventDto.getLatitude());
        properties.put("longitude", eventDto.getLongitude());
        properties.put("deviceTime", eventDto.getDeviceTime() != null ? eventDto.getDeviceTime().toString() : null);

        Set<ApplicationEventCreate.NotificationInternal> notificationInternals = Collections.emptySet();
        Set<ApplicationEventCreate.NotificationExternal> notificationExternals = Collections.emptySet();

        if (geofenceConfig != null) {
            notificationInternals = parseInternals(geofenceConfig.getNotificationInternalsJson());
            notificationExternals = parseExternals(geofenceConfig.getNotificationExternalsJson());
        } else {
            log.debug("Sin config de notificación para geofence={}, IMEI={}", eventDto.getGeofenceId(), eventDto.getImei());
        }

        // Determinar el tipo de evento basado en el string del DTO
        EventTypeEnumerated eventType = "ENTRY".equals(eventDto.getEventType())
                ? EventTypeEnumerated.GEOFENCE_ENTERED
                : EventTypeEnumerated.GEOFENCE_EXITED;

        return ApplicationEventCreate.builder()
                .eventId(eventId)
                .aggregateId(eventDto.getGeofenceId().toString())
                .aggregateType("Geofence")
                .applicationName(ApplicationName.ms_core)
                .moduleName(ModuleName.device)
                .eventType(eventType)
                .companyId(eventDto.getCompanyId())
                .title(title)
                .description(description)
                .properties(properties)
                .notificationInternals(notificationInternals)
                .notificationExternals(notificationExternals)
                .resolved(false)
                .resolvedTime(null)
                .build();
    }

    private static String buildTitle(GeofenceEventDto eventDto) {
        String action = "ENTRY".equals(eventDto.getEventType()) ? "entrada" : "salida";
        return String.format("Geocerca: %s — %s detectada para IMEI %s",
                eventDto.getGeofenceName(), action, eventDto.getImei());
    }

    private static String buildDescription(GeofenceEventDto eventDto) {
        return String.format("El dispositivo %s %s la geocerca '%s' en (%.5f, %.5f)",
                eventDto.getImei(),
                "ENTRY".equals(eventDto.getEventType()) ? "ingresó a" : "salió de",
                eventDto.getGeofenceName(),
                eventDto.getLatitude(),
                eventDto.getLongitude());
    }

    @SuppressWarnings("unchecked")
    private static Set<ApplicationEventCreate.NotificationInternal> parseInternals(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptySet();
        try {
            List<Map<String, Object>> list = MAPPER.readValue(json, new TypeReference<>() {});
            return list.stream()
                    .map(m -> {
                        Long userId = m.get("userId") != null
                                ? ((Number) m.get("userId")).longValue()
                                : null;
                                
                        List<String> typeStrings = m.get("notificationTypes") instanceof List
                                ? (List<String>) m.get("notificationTypes")
                                : Collections.emptyList();
                                
                        Set<NotificationTypes> enumTypes = typeStrings.stream()
                                .map(typeStr -> {
                                    try {
                                        return NotificationTypes.valueOf(typeStr);
                                    } catch (IllegalArgumentException ex) {
                                        log.warn("NotificationType desconocida en internal: {}", typeStr);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                        return ApplicationEventCreate.NotificationInternal.builder()
                                .userId(userId)
                                .notificationTypes(enumTypes) 
                                .build();
                    })
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Error parseando notificationInternals JSON: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<ApplicationEventCreate.NotificationExternal> parseExternals(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptySet();
        try {
            List<Map<String, Object>> list = MAPPER.readValue(json, new TypeReference<>() {});
            return list.stream()
                    .map(m -> {
                        String typeStr = (String) m.get("notificationType");
                        NotificationTypes type = null;
                        if (typeStr != null) {
                            try {
                                type = NotificationTypes.valueOf(typeStr);
                            } catch (IllegalArgumentException ex) {
                                log.warn("NotificationType desconocida: {}", typeStr);
                            }
                        }
                        List<String> dest = m.get("destination") instanceof List
                                ? (List<String>) m.get("destination")
                                : Collections.emptyList();
                        return ApplicationEventCreate.NotificationExternal.builder()
                                .notificationType(type)
                                .destination(new HashSet<>(dest))
                                .build();
                    })
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Error parseando notificationExternals JSON: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
}
