package com.ingestion.pe.mscore.commons.models.enums;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public enum NotificationTypes {
  sms("Notifiacion por sms"),
  email("Notificacion por email"),
  whatsapp("Notificacion por whatsapp"),
  web("Notificacion por la web"),
  push("Notificación push");
  private final String description;

  NotificationTypes(String description) {
    this.description = description;
  }

  public static Map<String, String> getTypesNotification() {
    Map<String, String> notificationTypesMap = new HashMap<>();
    for (NotificationTypes notificationTypes : NotificationTypes.values()) {
      notificationTypesMap.put(notificationTypes.name(), notificationTypes.getDescription());
    }
    return notificationTypesMap;
  }
}
