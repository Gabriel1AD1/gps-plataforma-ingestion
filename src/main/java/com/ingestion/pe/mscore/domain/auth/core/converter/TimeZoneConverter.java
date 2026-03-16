package com.ingestion.pe.mscore.domain.auth.core.converter;

import com.ingestion.pe.mscore.commons.converter.EnumAttributeConverter;
import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import jakarta.persistence.Converter;

@Converter
public class TimeZoneConverter extends EnumAttributeConverter<TimeZone> {

  public TimeZoneConverter() {
    super(TimeZone.class);
  }
}
