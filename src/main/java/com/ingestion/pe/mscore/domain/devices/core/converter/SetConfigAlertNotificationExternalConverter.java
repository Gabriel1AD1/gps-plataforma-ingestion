package com.ingestion.pe.mscore.domain.devices.core.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.domain.devices.core.models.ConfigAlertNotificationExternal;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Set;

@Converter
public class SetConfigAlertNotificationExternalConverter
        implements AttributeConverter<Set<ConfigAlertNotificationExternal>, String> {
    @Override
    public String convertToDatabaseColumn(Set<ConfigAlertNotificationExternal> attribute) {
        return JsonUtils.toJson(attribute);
    }

    @Override
    public Set<ConfigAlertNotificationExternal> convertToEntityAttribute(String dbData) {
        return JsonUtils.toSet(dbData, ConfigAlertNotificationExternal.class);
    }
}
