package com.ingestion.pe.mscore.domain.devices.app.handlers.models;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceStatus;
import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatusDevice {
    private Long deviceId;
    private Boolean resetDevices;
    private String imei;
    private DeviceStatus status;

    public static StatusDevice fromJson(String json) {
        return JsonUtils.toObject(json, StatusDevice.class);
    }
}
