package com.ingestion.pe.mscore.domain.devices.app.factory;

import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.devices.core.dto.response.DevicesStatusSummary;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import java.time.Instant;
import java.util.*;

public final class DeviceWebsocketMessageRefreshFactory {

        private DeviceWebsocketMessageRefreshFactory() {
        }

        /*
         * =========================
         * Casos de uso públicos
         * =========================
         */

        public static WebsocketMessage newDeviceStatus(DeviceEntity device) {
                return refresh(
                                "STATUS_DEVICE",
                                "Nuevo status para el dispositivo " + device.getImei(),
                                Map.of(
                                                "deviceId",
                                                device.getId(),
                                                "status",
                                                device.getDeviceStatus().name(),
                                                "updateAt",
                                                Instant.now().toString()));
        }

        public static WebsocketMessage newDeviceUpdate(
                        Long companyId, DeviceEntity device, HistoricalDeviceEntity historicalSave) {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("deviceId", device.getId());
                attributes.put("imei", device.getImei());
                attributes.put("historicalId", historicalSave.getId());
                attributes.put("status", device.getStatus());
                attributes.put("sensor", device.getSensor());
                attributes.put("latitude", historicalSave.getLatitude());
                attributes.put("longitude", historicalSave.getLongitude());
                attributes.put("speedInKm", historicalSave.getSpeedInKm());
                attributes.put("course", historicalSave.getCourse());
                attributes.put("address", historicalSave.getAddress());
                attributes.put("severTime", historicalSave.getServerTime().toString());
                attributes.put("deviceTime", historicalSave.getDeviceTime().toString());
                attributes.put("fixTime", historicalSave.getFixTime().toString());
                attributes.put("sensorsRaw", device.getSensorRaw());
                attributes.put("sensorData", device.getSensorsData());
                attributes.put("updateAt", Instant.now().toString());
                return refresh(
                                "DEVICE_UPDATE",
                                "Nueva posición registrada",
                                attributes);
        }

        public static WebsocketMessage newSummaryDevice(
                        Long companyId, List<DevicesStatusSummary> summaryStatusSystems) {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put(
                                "totalDevices",
                                summaryStatusSystems.stream().mapToLong(DevicesStatusSummary::getCountDevices).sum());
                attributes.put("online", 0L);
                attributes.put("offline", 0L);
                attributes.put("unknown", 0L);
                attributes.put("updateAt", Instant.now().toString());
                summaryStatusSystems.forEach(
                                deviceSummary -> attributes.put(deviceSummary.getStatus().name(),
                                                deviceSummary.getCountDevices()));
                return refresh(
                                "SUMMARY_DEVICE",
                                "Resumen de dispositivos actualizado",
                                attributes);
        }

        /*
         * =========================
         * Factory base reutilizable
         * =========================
         */

        private static WebsocketMessage refresh(
                        String type,
                        String message,
                        Map<String, Object> properties) {

                return WebsocketMessage.refreshBuilder()
                                .message(message)
                                .properties(properties)
                                .messageAgregateType(WebsocketMessage.MessageAgregateType.valueOf(type))
                                .build();
        }
}
