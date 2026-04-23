package com.ingestion.pe.mscore.bridge.pub.models;

import com.ingestion.pe.mscore.commons.models.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

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
