package com.ingestion.pe.mscore.domain.vehicles.app.factory;

import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.vehicles.core.dto.response.VehicleStatusSummary;
import com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleTrackingEntity;
import com.ingestion.pe.mscore.domain.vehicles.core.models.SnapshotManager;

import java.util.*;

public final class VehicleWebsocketMessageFactory {

  private VehicleWebsocketMessageFactory() {
  }

  public static WebsocketMessage newVehicleUpdate(DeviceEntity deviceEntity, VehicleTrackingEntity vehicle) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("vehicleId", vehicle.getId());
    attributes.put("deviceId", vehicle.getDeviceId());
    attributes.put("imei", deviceEntity.getImei());
    attributes.put("status", vehicle.getStatus() != null ? vehicle.getStatus().name() : "unknown");
    attributes.put(
      "snapshot",
      Optional.ofNullable(vehicle.getSnapshotManager())
        .orElse(SnapshotManager.defaultSnapshot()));
    attributes.put("odometerKm", vehicle.getOdometerKm());
    attributes.put("sensors", vehicle.getSensors());
    attributes.put("sensorsData", deviceEntity.getSensorDataMap());
    
    return WebsocketMessage.refreshBuilder()
      .messageAgregateType(WebsocketMessage.MessageAgregateType.VEHICLE_UPDATE)
      .message("Actualización de seguimiento de vehículo")
      .properties(attributes)
      .companyId(vehicle.getCompanyId())
      .build();
  }

  public static WebsocketMessage newVehicleStatusSummary(VehicleTrackingEntity vehicle, List<VehicleStatusSummary> vehicleStatusSummaries) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("totalVehicles", vehicleStatusSummaries.size());
    attributes.put("summary", vehicleStatusSummaries);
    return WebsocketMessage.refreshBuilder()
      .messageAgregateType(WebsocketMessage.MessageAgregateType.VEHICLE_STATUS_SUMMARY)
      .message("Actualización de resumen de estado de los vehículos")
      .properties(attributes)
      .companyId(vehicle.getCompanyId())
      .build();
  }

  public static WebsocketMessage newVehicleStatus(VehicleTrackingEntity vehicle) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("vehicleId", vehicle.getId());
    attributes.put("status", vehicle.getStatus() != null ? vehicle.getStatus().name() : "unknown");
    return WebsocketMessage.refreshBuilder()
      .messageAgregateType(WebsocketMessage.MessageAgregateType.VEHICLE_STATUS_UPDATE)
      .message("Actualización de estado del vehículo")
      .properties(attributes)
      .companyId(vehicle.getCompanyId())
      .build();
  }
}
