package com.ingestion.pe.mscore.domain.devices.app.factory;

import com.ingestion.pe.mscore.bridge.pub.models.ApplicationEventCreate;
import com.ingestion.pe.mscore.bridge.pub.models.enums.ApplicationName;
import com.ingestion.pe.mscore.bridge.pub.models.enums.ModuleName;
import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.commons.models.enums.EventTypeEnumerated;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.models.ConfigAlertNotificationExternal;
import com.ingestion.pe.mscore.domain.devices.core.models.ConfigAlertNotificationInternal;
import java.util.*;
import java.util.stream.Collectors;

public class DeviceApplicationEventFactory {

        public static ApplicationEventCreate.ApplicationEventCreateBuilder configAlert() {
                return ApplicationEventCreate.builder()
                                .aggregateType("DeviceConfigAlerts")
                                .applicationName(ApplicationName.ms_core)
                                .moduleName(ModuleName.device)
                                .eventType(EventTypeEnumerated.CONFIG_ALERT);
        }

        public static ApplicationEventCreate newConfigAlertNotResolved(
                        DeviceEntity device,
                        Set<UUID> usersNotSendPositions,
                        Long companyId,
                        DeviceConfigAlertsEntity triggeredAlertsFor,
                        List<VehicleResponse> vehicleAsociateForDevice) {

                String vehiclesText = vehicleAsociateForDevice.stream()
                                .map(
                                                v -> String.format(
                                                                "Vehiculo %s | %s %s | Placa: %s | Vel: %.1f km/h | Ubicacion: (%.5f, %.5f)",
                                                                v.getId(),
                                                                v.getBrand(),
                                                                v.getModel(),
                                                                v.getLicensePlate(),
                                                                device.getSpeedInKmh(),
                                                                device.getLatitude(),
                                                                device.getLongitude()))
                                .collect(Collectors.joining("\n"));

                Map<String, Object> properties = Map.of(
                                "deviceId",
                                device.getId(),
                                "imei",
                                device.getImei(),
                                "alertId",
                                triggeredAlertsFor.getConfigAlerts().getId(),
                                "alertTitle",
                                triggeredAlertsFor.getConfigAlerts().getTitle(),
                                "alertDescription",
                                triggeredAlertsFor.getConfigAlerts().getDescription(),
                                "currentSensors",
                                device.getSensor(),
                                "vehicles",
                                vehiclesText);
                var notificationsExternals = triggeredAlertsFor.getConfigAlerts().getNotificationExternals();
                var notificationsInternals = triggeredAlertsFor.getConfigAlerts().getNotificationInternals();
                var notificationsInternalsMap = getNotificationsInternalsMap(notificationsInternals);
                var notificationsExternalsMap = getNotificationsExternalsMap(notificationsExternals);
                return configAlert()
                                .latitude(device.getLatitude())
                                .longitude(device.getLongitude())
                                .userExcludeIds(
                                                usersNotSendPositions.stream().map(UUID::toString)
                                                                .collect(Collectors.toSet()))
                                .companyId(companyId)
                                .notificationExternals(notificationsExternalsMap)
                                .notificationInternals(notificationsInternalsMap)
                                .eventId(triggeredAlertsFor.getEventId() != null
                                                ? triggeredAlertsFor.getEventId().toString()
                                                : UUID.randomUUID().toString())
                                .aggregateId(triggeredAlertsFor.getId().toString())
                                .title(triggeredAlertsFor.getConfigAlerts().getTitle())
                                .description(triggeredAlertsFor.getConfigAlerts().getDescription())
                                .properties(properties)
                                .resolved(false)
                                .resolvedTime(null)
                                .build();
        }

        private static Set<ApplicationEventCreate.NotificationInternal> getNotificationsInternalsMap(
                        Set<ConfigAlertNotificationInternal> notificationsInternals) {
                return notificationsInternals.stream()
                                .map(
                                                n -> ApplicationEventCreate.NotificationInternal.builder()
                                                                .notificationTypes(n.getNotificationTypes())
                                                                .userId(n.getUserId())
                                                                .build())
                                .collect(Collectors.toSet());
        }

        private static Set<ApplicationEventCreate.NotificationExternal> getNotificationsExternalsMap(
                        Set<ConfigAlertNotificationExternal> notificationsExternals) {
                return notificationsExternals.stream()
                                .map(
                                                n -> ApplicationEventCreate.NotificationExternal.builder()
                                                                .notificationType(n.getNotificationType())
                                                                .destination(n.getDestination())
                                                                .build())
                                .collect(Collectors.toSet());
        }
}
