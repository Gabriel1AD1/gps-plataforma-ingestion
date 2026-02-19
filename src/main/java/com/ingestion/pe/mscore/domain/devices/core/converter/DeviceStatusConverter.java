package com.ingestion.pe.mscore.domain.devices.core.converter;

import com.ingestion.pe.mscore.commons.converter.EnumAttributeConverter;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceStatus;
import jakarta.persistence.Converter;

@Converter
public class DeviceStatusConverter extends EnumAttributeConverter<DeviceStatus> {
    public DeviceStatusConverter() {
        super(DeviceStatus.class);
    }
}
