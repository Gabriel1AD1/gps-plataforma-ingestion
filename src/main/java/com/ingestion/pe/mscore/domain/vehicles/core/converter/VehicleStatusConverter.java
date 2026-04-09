package com.ingestion.pe.mscore.domain.vehicles.core.converter;

import com.ingestion.pe.mscore.domain.vehicles.core.enums.VehicleStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class VehicleStatusConverter implements AttributeConverter<VehicleStatus, String> {
  @Override
  public String convertToDatabaseColumn(VehicleStatus attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.name();
  }

  @Override
  public VehicleStatus convertToEntityAttribute(String dbData) {
    return VehicleStatus.fromString(dbData);
  }
}
