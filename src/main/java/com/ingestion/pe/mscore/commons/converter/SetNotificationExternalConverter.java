package com.ingestion.pe.mscore.commons.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.commons.models.events.EventNotificationExternal;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Set;

@Converter
public class SetNotificationExternalConverter implements
  AttributeConverter<Set<EventNotificationExternal>,String> {
  @Override
  public String convertToDatabaseColumn(Set<EventNotificationExternal> attribute) {
    return JsonUtils.toJson(attribute);
  }

  @Override
  public Set<EventNotificationExternal> convertToEntityAttribute(String dbData) {
    return JsonUtils.toSet(dbData, EventNotificationExternal.class);
  }
}
