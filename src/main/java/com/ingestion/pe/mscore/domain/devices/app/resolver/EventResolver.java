package com.ingestion.pe.mscore.domain.devices.app.resolver;

import com.ingestion.pe.mscore.clients.cache.UserCache;
import com.ingestion.pe.mscore.clients.models.UserResponse;
import com.ingestion.pe.mscore.config.kafka.pub.KafkaNotificationProducer;
import com.ingestion.pe.mscore.domain.devices.core.entity.EventEntity;
import com.ingestion.pe.mscore.commons.models.events.EventNotificationInternal;
import com.ingestion.pe.mscore.commons.models.events.NotificationData;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventResolver {

  private final UserCache userCache;
  private final KafkaNotificationProducer notificationProducer;

  @Async
  public void resolveEvent(EventEntity eventEntity) {
    NotificationData notificationData = new NotificationData();
    List<Long> internals =
        eventEntity.getNotificationInternals().stream()
            .map(EventNotificationInternal::getUserId)
            .toList();

    List<UserResponse> users = userCache.getUsersByIds(internals);

    Set<NotificationData.NotificationInternal> notificationInternals =
        users.stream()
            .map(
                user ->
                    NotificationData.NotificationInternal.builder()
                        .emails(user.getEmails())
                        .phones(user.getPhones())
                        .pushTokens(user.getPushTokens())
                        .timeZone(user.getTimeZone())
                        .notificationTypes(
                            eventEntity.getNotificationInternals().stream()
                                .filter(internal -> internal.getUserId().equals(user.getId()))
                                .flatMap(internal -> internal.getNotificationTypes().stream())
                                .collect(Collectors.toSet()))
                        .build())
            .collect(Collectors.toSet());

    notificationData.setInternals(notificationInternals);

    Set<NotificationData.NotificationExternal> notificationExternals =
        eventEntity.getNotificationExternals().stream()
            .map(
                external ->
                    NotificationData.NotificationExternal.builder()
                        .notificationType(external.getNotificationType())
                        .destination(external.getDestinations())
                        .timeZone(external.getTimeZones())
                        .build())
            .collect(Collectors.toSet());
    notificationData.setExternals(notificationExternals);

    notificationData.setMessage(
        NotificationData.Message.builder()
            .eventId(eventEntity.getId().toString())
            .title(eventEntity.getTitle())
            .description(eventEntity.getDescription())
            .severityStatus(eventEntity.getStatus())
            .occurredAt(eventEntity.getOccurredAt())
            .correlationId(eventEntity.getCorrelationId())
            .enable(eventEntity.getActive())
            .properties(eventEntity.getProperties())           
            .resolutionProperties(eventEntity.getResolutionProperties()) 
            .build());

    notificationProducer.send(notificationData);
  }
}
