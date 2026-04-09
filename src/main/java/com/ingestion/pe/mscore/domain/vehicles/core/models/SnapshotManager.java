package com.ingestion.pe.mscore.domain.vehicles.core.models;

import com.ingestion.pe.mscore.domain.vehicles.core.enums.IgnitionStatus;
import com.ingestion.pe.mscore.domain.vehicles.core.enums.VehicleStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SnapshotManager {
  private String correlationId;
  private VehicleStatus vehicleStatus;
  private Double latitude;
  private Double longitude;
  private Double altitude;
  private Double satellite;
  private Double horizontalDilutionOfPrecision;
  private Instant recordedAt;
  private IgnitionStatus ignitionStatus; // ON / OFF
  private Instant deviceTime;
  private Instant serverTime;
  private Double speedInKmh;

  public static SnapshotManager of(
      String correlationId,
      VehicleStatus vehicleStatus,
      Double satellite,
      Double horizontalDilutionOfPrecision,
      IgnitionStatus ignitionStatus,
      Double speedInKmh,
      Double latitude,
      Double longitude,
      Double altitude,
      Instant deviceTime) {
    SnapshotManager snapshot = new SnapshotManager();
    snapshot.setCorrelationId(correlationId);
    snapshot.setHorizontalDilutionOfPrecision(horizontalDilutionOfPrecision);
    snapshot.setVehicleStatus(vehicleStatus);
    snapshot.setSatellite(satellite);
    snapshot.setIgnitionStatus(ignitionStatus);
    snapshot.setSpeedInKmh(speedInKmh);
    snapshot.setLatitude(latitude);
    snapshot.setLongitude(longitude);
    snapshot.setAltitude(altitude);
    snapshot.setRecordedAt(Instant.now());
    snapshot.setDeviceTime(deviceTime);
    snapshot.setServerTime(Instant.now());
    return snapshot;
  }

  public static SnapshotManager defaultSnapshot() {
    return SnapshotManager.builder()
        .vehicleStatus(VehicleStatus.offline)
        .ignitionStatus(IgnitionStatus.off)
        .satellite(0.0)
        .speedInKmh(0.0)
        .latitude(0.0)
        .longitude(0.0)
        .altitude(0.0)
        .recordedAt(Instant.now())
        .deviceTime(Instant.now())
        .serverTime(Instant.now())
        .build();
  }
}
