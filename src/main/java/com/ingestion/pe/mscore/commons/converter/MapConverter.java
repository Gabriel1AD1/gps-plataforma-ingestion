package com.ingestion.pe.mscore.commons.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
public class MapConverter implements AttributeConverter<Map<String, Object>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Object> o) {
        return JsonUtils.toJson(o);
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String o) {
        return JsonUtils.convertAttributesToMap(o);
    }
}
