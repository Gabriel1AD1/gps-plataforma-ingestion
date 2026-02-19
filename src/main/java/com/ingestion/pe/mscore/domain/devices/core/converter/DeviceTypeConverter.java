package com.ingestion.pe.mscore.domain.devices.core.converter;

import com.ingestion.pe.mscore.commons.converter.EnumAttributeConverter;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceType;
import jakarta.persistence.Convert;

@Convert
public class DeviceTypeConverter extends EnumAttributeConverter<DeviceType> {
    public DeviceTypeConverter() {
        super(DeviceType.class);
    }
}
