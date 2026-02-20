package com.ingestion.pe.mscore.domain.devices.core.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.domain.devices.core.models.ConfigAlertNotificationInternal;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Set;

@Converter
public class SetConfigAlertNotificationInternalConverter
        implements AttributeConverter<Set<ConfigAlertNotificationInternal>, String> {
    @Override
    public String convertToDatabaseColumn(Set<ConfigAlertNotificationInternal> attribute) {
        return JsonUtils.toJson(attribute);
    }

    @Override
    public Set<ConfigAlertNotificationInternal> convertToEntityAttribute(String dbData) {
        return JsonUtils.toSet(dbData, ConfigAlertNotificationInternal.class);
    }
}
