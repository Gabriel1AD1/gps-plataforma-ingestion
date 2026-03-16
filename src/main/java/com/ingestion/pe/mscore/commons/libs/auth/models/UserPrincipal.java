package com.ingestion.pe.mscore.commons.libs.auth.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ingestion.pe.mscore.commons.models.enums.Role;
import com.ingestion.pe.mscore.commons.models.enums.Scopes;
import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPrincipal {
  private static final Logger log = LoggerFactory.getLogger(UserPrincipal.class);
  private Long id;
  private Long companyId;
  private String email;
  private UUID uuid;
  private TimeZone timeZone;
  private Set<Scopes> scopes;
  private String ipAddress;
  private List<Role> roles;

  private String requestId;

  public void log() {
    log.info(this.toString());
  }

  public boolean compareUserIdAndCompanyId(UserPrincipal other) {
    if (other == null) {
      return false;
    }
    return this.id.equals(other.id);
  }

  @Override
  public String toString() {
    return "UserPrincipal{"
        + "id="
        + id
        + ", companyId="
        + companyId
        + ", email='"
        + email
        + '\''
        + ", uuid="
        + uuid
        + ", timeZone="
        + timeZone
        + ", ipAddress='"
        + ipAddress
        + '\''
        + ", roles="
        + roles
        + '}';
  }
}
