package com.ingestion.pe.mscore.clients.models;

import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleResponse implements Serializable {
    private Long id;
    private Object snapshotManager;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private String category;
    private String fuelType;
    private String status;
    private Long companyId;
    private Long deviceId;
    private Integer engineCapacity;
    private String registrationNumber;
    private Instant registrationExpiration;
    private Double odometerKm;
    private String description;
    private Instant created;
    private Instant updated;
}
