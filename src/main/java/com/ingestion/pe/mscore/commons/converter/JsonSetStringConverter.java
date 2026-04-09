package com.ingestion.pe.mscore.commons.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Set;

@Converter
public class JsonSetStringConverter implements AttributeConverter<Set<String>, String> {

  @Override
  public String convertToDatabaseColumn(Set<String> attribute) {
    return JsonUtils.toJson(attribute);
  }

  @Override
  public Set<String> convertToEntityAttribute(String dbData) {
    return JsonUtils.toSet(dbData, String.class);
  }
}
