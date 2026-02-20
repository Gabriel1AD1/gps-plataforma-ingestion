package com.ingestion.pe.mscore.commons.models;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeofenceResponse implements Serializable {
    private Long id;
    private Long companyId;
    private String name;
    private String description;
    private Double latitudeCenter;
    private Double longitudeCenter;
    private Double radiusInMeters;
    private String type;
    private List<PointsResponse> points;
}
