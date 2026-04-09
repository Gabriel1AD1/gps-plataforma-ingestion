package com.ingestion.pe.mscore.domain.vehicles.core.entity;

import com.ingestion.pe.mscore.commons.converter.MapConverter;
import com.ingestion.pe.mscore.domain.vehicles.core.converter.ListSnapShotManagerConverter;
import com.ingestion.pe.mscore.domain.vehicles.core.converter.SnapShotManagerConverter;
import com.ingestion.pe.mscore.domain.vehicles.core.converter.VehicleStatusConverter;
import com.ingestion.pe.mscore.domain.vehicles.core.enums.VehicleStatus;
import com.ingestion.pe.mscore.domain.vehicles.core.models.SnapshotManager;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "vehicle", schema = "vehicle_module")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleTrackingEntity {
  @Id
  private Long id;

  @Column(name = "license_plate", insertable = false, updatable = false)
  private String licensePlate;

  @Column(name = "brand", insertable = false, updatable = false)
  private String brand;

  @Column(name = "snapshot_manager", columnDefinition = "TEXT")
  @Convert(converter = SnapShotManagerConverter.class)
  private SnapshotManager snapshotManager;

  @Column(name = "snapshots_manager", columnDefinition = "TEXT")
  @Convert(converter = ListSnapShotManagerConverter.class)
  private List<SnapshotManager> snapshotsManager;

  @Column(name = "model", insertable = false, updatable = false)
  private String model;

  @Column(name = "year", insertable = false, updatable = false)
  private Integer year;

  @Column(name = "color", insertable = false, updatable = false)
  private String color;

  @Column(name = "status")
  @Convert(converter = VehicleStatusConverter.class)
  private VehicleStatus status;

  @Column(name = "company_id", insertable = false, updatable = false)
  private Long companyId;

  @Column(name = "device_id", insertable = false, updatable = false)
  private Long deviceId;

  @Column(name = "odometer_km")
  private Double odometerKm;

  @Column(name = "sensors", length = 4000)
  @Convert(converter = MapConverter.class)
  @ColumnDefault("'{}'")
  private Map<String, Object> sensors;

  @Column(name = "updated")
  private Instant updated;

  public void addSnapshotManager(SnapshotManager snapshotManager) {
    this.snapshotManager = snapshotManager;
    if (this.snapshotsManager == null) {
      this.snapshotsManager = new java.util.LinkedList<>();
    }
    
    if (snapshotManager == null) {
      return;
    }

    this.snapshotsManager.add(snapshotManager);

    while (this.snapshotsManager.size() > 50) {
      this.snapshotsManager.remove(0);
    }
  }
}
