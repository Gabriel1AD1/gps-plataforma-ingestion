package com.ingestion.pe.mscore.domain.vehicles.core.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.domain.vehicles.core.models.SnapshotManager;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SnapShotManagerConverter implements AttributeConverter<SnapshotManager, String> {
  @Override
  public String convertToDatabaseColumn(SnapshotManager attribute) {
    return JsonUtils.toJson(attribute);
  }

  @Override
  public SnapshotManager convertToEntityAttribute(String dbData) {
    try {
      return JsonUtils.toObject(dbData, SnapshotManager.class);
    } catch (Exception e) {
      return SnapshotManager.defaultSnapshot();
    }
  }
}
