package com.ingestion.pe.mscore.bridge.pub.models;

import com.ingestion.pe.mscore.commons.models.Position;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DevicePositionEventCreate {
    private Long deviceId;
    private String imei;
    private Map<String, Object> sensors;
    private Set<Map<String, Object>> sensorData;
    private Position position;
}
