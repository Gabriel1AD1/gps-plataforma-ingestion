package com.ingestion.pe.mscore.domain.monitoring.core.model;

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
public class LinearViewResult implements Serializable {
    private Long tripId;
    private Long vehicleId;
    private Long driverId;
    private Double progressPercent; // 0.0 a 100.0
    private String status; // A tiempo, Retrasado, Adelantado
    private String direction;
    private Long aheadVehicleId;
    private Double aheadDeltaKm;
    private Double aheadDeltaMinutes;
    private Long behindVehicleId;
    private Double behindDeltaKm;
    private Double behindDeltaMinutes;
    private Instant lastUpdateTime;
}
