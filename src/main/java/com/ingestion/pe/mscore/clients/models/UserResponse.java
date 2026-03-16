package com.ingestion.pe.mscore.clients.models;

import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
  private Long id;
  private String email;
  private Set<String> emails;
  private Set<String> phones;
  private Set<String> pushTokens;
  private TimeZone timeZone;
}
