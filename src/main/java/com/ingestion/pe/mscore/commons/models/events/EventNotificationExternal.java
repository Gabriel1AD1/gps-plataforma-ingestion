package com.ingestion.pe.mscore.commons.models.events;

import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import com.ingestion.pe.mscore.commons.models.enums.NotificationTypes;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventNotificationExternal {
  private NotificationTypes notificationType;
  private Set<String> destinations;
  private TimeZone timeZones;
}
