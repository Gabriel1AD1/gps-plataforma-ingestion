package com.ingestion.pe.mscore.domain.devices.core.dto.response;

import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class DevicesStatusSummary {
    private Long countDevices;
    private DeviceStatus status;

    public DevicesStatusSummary(Long countDevices, DeviceStatus status) {
        this.countDevices = countDevices;
        this.status = status;
    }
}
