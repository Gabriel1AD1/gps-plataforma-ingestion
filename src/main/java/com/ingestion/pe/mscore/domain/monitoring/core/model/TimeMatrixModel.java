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
public class TimeMatrixModel implements Serializable {
    private Long fromControlPointId;
    private Long toControlPointId;
    private Long timeSpanId;
    private Double expectedTravelMinutes;
    private String direction;
}
