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
public class ControlPointModel implements Serializable {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Integer sequenceOrder;
    private String direction; 
    private Double distanceFromStartKm;
    private Long terminalId;
    private Long geofenceId;
}
