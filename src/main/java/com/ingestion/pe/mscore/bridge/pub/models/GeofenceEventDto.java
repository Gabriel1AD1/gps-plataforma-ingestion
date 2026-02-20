package com.ingestion.pe.mscore.bridge.pub.models;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeofenceEventDto {
    private String imei;
    private Long companyId;
    private Long geofenceId;
    private String geofenceName;
    private String eventType;
    private Double latitude;
    private Double longitude;
    private Instant deviceTime;
}
