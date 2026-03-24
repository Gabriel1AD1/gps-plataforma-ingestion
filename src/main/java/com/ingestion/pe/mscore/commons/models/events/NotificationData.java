package com.ingestion.pe.mscore.commons.models.events;

import com.ingestion.pe.mscore.commons.models.enums.SeverityStatus;
import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import com.ingestion.pe.mscore.commons.models.enums.NotificationTypes;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationData {
  private Set<NotificationInternal> internals;
  private Set<NotificationExternal> externals;
  private Message message;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NotificationInternal {
    private Set<NotificationTypes> notificationTypes;
    private Set<String> emails;
    private Set<String> phones;
    private Set<String> pushTokens;
    private TimeZone timeZone;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NotificationExternal {
    private NotificationTypes notificationType;
    private Set<String> destination;
    private TimeZone timeZone;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Message {
    private String eventId;
    private Map<String, Object> properties;
    private String description;
    private String title;
    private SeverityStatus severityStatus;
    private Instant occurredAt;
    private Boolean enable;
    private String correlationId;
    private Map<String, Object> resolutionProperties;
  }
}
