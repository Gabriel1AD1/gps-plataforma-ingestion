package com.ingestion.pe.mscore.domain.monitoring.core.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouteConfigResponse implements Serializable {
    private Long routeId;
    private Double totalDistanceKm;
    private Integer frequencyMinutes;
    private String type;
    private List<ControlPointModel> controlPoints;
    private List<TimeMatrixModel> timeMatrix;
    private List<TimeSpanModel> timeSpans;
}
