package com.ingestion.pe.mscore.commons.models.enums;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;

@Getter
public enum TimeZone {
  UTC("UTC", "Coordinated Universal Time"),
  AMERICA_MEXICO_CITY("America/Mexico_City", "Ciudad de México"),
  AMERICA_BOGOTA("America/Bogota", "Bogotá"),
  AMERICA_LIMA("America/Lima", "Lima"),
  AMERICA_QUITO("America/Guayaquil", "Quito"),
  AMERICA_CARACAS("America/Caracas", "Caracas"),
  AMERICA_SANTIAGO("America/Santiago", "Santiago de Chile"),
  AMERICA_BUENOS_AIRES("America/Argentina/Buenos_Aires", "Buenos Aires"),
  AMERICA_ASUNCION("America/Asuncion", "Asunción"),
  AMERICA_LA_PAZ("America/La_Paz", "La Paz"),
  AMERICA_MONTEVIDEO("America/Montevideo", "Montevideo"),
  AMERICA_PANAMA("America/Panama", "Panamá"),
  AMERICA_PUERTO_RICO("America/Puerto_Rico", "Puerto Rico"),
  AMERICA_SAO_PAULO("America/Sao_Paulo", "São Paulo"),
  AMERICA_CUIABA("America/Cuiaba", "Cuiabá"),
  AMERICA_MANAUS("America/Manaus", "Manaos"),
  AMERICA_RECIFE("America/Recife", "Recife"),

  // Otros comunes
  US_EASTERN("America/New_York", "US Eastern"),
  US_PACIFIC("America/Los_Angeles", "US Pacific"),
  EUROPE_MADRID("Europe/Madrid", "Madrid"),
  EUROPE_LONDON("Europe/London", "Londres");

  private final String zoneId;
  private final String displayName;

  TimeZone(String zoneId, String displayName) {
    this.zoneId = zoneId;
    this.displayName = displayName;
  }

  public ZoneId toZoneId() {
    return java.time.ZoneId.of(zoneId);
  }

  public LocalDateTime convertLocalDateTime(Instant instant) {
    return LocalDateTime.ofInstant(instant, toZoneId());
  }
}
