package com.ingestion.pe.mscore.domain.devices.core.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.domain.devices.core.models.SensorModel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashSet;
import java.util.Set;

@Converter
public class SensorModelConverter implements AttributeConverter<Set<SensorModel>, String> {
    @Override
    public String convertToDatabaseColumn(Set<SensorModel> attribute) {
        return JsonUtils.toJson(attribute);
    }

    @Override
    public Set<SensorModel> convertToEntityAttribute(String dbData) {
        return new HashSet<>(JsonUtils.toSet(dbData, SensorModel.class));
    }
}
