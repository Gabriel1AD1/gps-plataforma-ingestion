package com.ingestion.pe.mscore.clients.models;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceResponse {
    private Long id;
    private String imei;
    private String serialNumber;
    private String password;
    private String model;
    private Long company;
    private Map<String, Object> sensor;
    private Map<String, Object> sensorRaw;
    private List<Map<String, Object>> dataHistory;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speedInKmh;
    private List<Map<String, Object>> sensorDataMap;
}
