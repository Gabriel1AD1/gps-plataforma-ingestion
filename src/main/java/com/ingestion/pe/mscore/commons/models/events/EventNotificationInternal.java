package com.ingestion.pe.mscore.commons.models.events;

import com.ingestion.pe.mscore.commons.models.enums.NotificationTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventNotificationInternal {
  private Set<NotificationTypes> notificationTypes;
  private Long userId;
}
