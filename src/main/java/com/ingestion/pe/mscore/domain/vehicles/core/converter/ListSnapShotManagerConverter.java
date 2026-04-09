package com.ingestion.pe.mscore.domain.vehicles.core.converter;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.domain.vehicles.core.models.SnapshotManager;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class ListSnapShotManagerConverter implements AttributeConverter<List<SnapshotManager>, String> {
  @Override
  public String convertToDatabaseColumn(List<SnapshotManager> attribute) {
    return JsonUtils.toJson(attribute);
  }

  @Override
  public List<SnapshotManager> convertToEntityAttribute(String dbData) {
    return JsonUtils.toList(dbData, SnapshotManager.class);
  }
}
