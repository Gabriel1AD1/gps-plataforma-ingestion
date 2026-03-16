package com.ingestion.pe.mscore.commons.models.enums;

import java.util.Set;

public enum Role {
  SUPPER_ADMIN,
  DRIVER,
  ADMIN,
  SUPPORT,
  ADMIN_TENANT,
  USER,
  UNDEFINED;

  public static Set<Role> getDefaultRoles() {
    return Set.of(
        Role.SUPPER_ADMIN, Role.DRIVER, Role.ADMIN, Role.SUPPORT, Role.ADMIN_TENANT, Role.USER);
  }
  public String getContent() {
    return this.name();
  }
  public static Role fromString(String dbData) {
    for (Role role : Role.values()) {
      if (role.name().equalsIgnoreCase(dbData)) {
        return role;
      }
    }
    return UNDEFINED;
  }
}
