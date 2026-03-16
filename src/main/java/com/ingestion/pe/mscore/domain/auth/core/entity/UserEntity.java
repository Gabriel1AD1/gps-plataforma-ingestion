package com.ingestion.pe.mscore.domain.auth.core.entity;

import com.ingestion.pe.mscore.commons.converter.JsonSetStringConverter;
import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import com.ingestion.pe.mscore.domain.auth.core.converter.TimeZoneConverter;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users", schema = "auth_module")
public class UserEntity {

  @Id
  @Column(nullable = false)
  private Long id;

  @Column(name = "email", insertable = false, updatable = false)
  private String email;

  @Column(name = "phones", insertable = false, updatable = false)
  @Convert(converter = JsonSetStringConverter.class)
  private Set<String> phones;

  @Column(name = "emails", insertable = false, updatable = false)
  @Convert(converter = JsonSetStringConverter.class)
  private Set<String> emails;

  @Column(name = "time_zone", insertable = false, updatable = false)
  @Convert(converter = TimeZoneConverter.class)
  private TimeZone timeZone;
}
