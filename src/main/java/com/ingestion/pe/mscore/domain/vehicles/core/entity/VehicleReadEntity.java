package com.ingestion.pe.mscore.domain.vehicles.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicle", schema = "vehicle_module")
public class VehicleReadEntity {

    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "device_id", insertable = false, updatable = false)
    private Long deviceId;

    @Column(name = "company_id", nullable = false, insertable = false, updatable = false)
    private Long companyId;

    @Column(name = "license_plate", nullable = false, insertable = false, updatable = false)
    private String licensePlate;

    @Column(insertable = false, updatable = false)
    private String brand;

    @Column(insertable = false, updatable = false)
    private String model;

    @Column(name = "\"year\"", insertable = false, updatable = false)
    private Integer year;

    @Column(insertable = false, updatable = false)
    private String color;

    @Column(insertable = false, updatable = false)
    private String status;

    @Column(name = "odometer_km", insertable = false, updatable = false)
    private Double odometerKm;

    @Column(insertable = false, updatable = false)
    private Instant created;

    @Column(insertable = false, updatable = false)
    private Instant updated;
}
