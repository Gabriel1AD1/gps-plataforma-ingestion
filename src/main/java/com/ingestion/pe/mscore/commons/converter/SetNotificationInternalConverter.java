package com.ingestion.pe.mscore.commons.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.commons.models.events.EventNotificationInternal;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Set;

@Converter
public class SetNotificationInternalConverter implements
  AttributeConverter<Set<EventNotificationInternal>,String> {
  @Override
  public String convertToDatabaseColumn(Set<EventNotificationInternal> attribute) {
    return JsonUtils.toJson(attribute);
  }

  @Override
  public Set<EventNotificationInternal> convertToEntityAttribute(String dbData) {
    return JsonUtils.toSet(dbData, EventNotificationInternal.class);
  }
}
