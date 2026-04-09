package com.ingestion.pe.mscore.domain.vehicles.core.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum VehicleStatus {
  online,
  in_route,
  stopped,
  offline,
  unknown,
  maintenance,
  ;

  public static VehicleStatus fromString(String dbData) {
    try {
      if (dbData == null) {
          return unknown;
      }
      return VehicleStatus.valueOf(dbData);
    } catch (Exception e) {
      log.warn("Unknown VehicleStatus: {}", dbData);
      return unknown;
    }
  }
}
