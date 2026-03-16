package com.ingestion.pe.mscore.domain.monitoring.core.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DateroResult implements Serializable {
    private Long tripId;
    private Long vehicleId;

    private Long aheadVehicleId;
    private Double aheadDeltaKm;
    private Double aheadDeltaMinutes;

    private Long behindVehicleId;
    private Double behindDeltaKm;
    private Double behindDeltaMinutes;

    private int rank; // Posición en la ruta (1 = líder)
    private String currentControlPointName;
    private int currentPointIndex;
}
