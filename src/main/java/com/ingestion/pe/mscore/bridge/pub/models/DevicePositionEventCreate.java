package com.ingestion.pe.mscore.bridge.pub.models;

import com.ingestion.pe.mscore.commons.models.Position;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DevicePositionEventCreate {
    private Long deviceId;
    private Instant deviceTime;
    private String imei;
    private Map<String, Object> sensors;
    private Set<Map<String, Object>> sensorData;
    private Position position;
}
