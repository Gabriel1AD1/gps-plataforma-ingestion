package com.ingestion.pe.mscore.config.kafka.pub;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.commons.models.events.NotificationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationProducer {

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${kafka.topic.notifications}")
  private String notificationsTopic;

  public void send(NotificationData notificationData) {
    if (notificationData == null) {
      log.warn("NotificationData es null, omitiendo envio");
      return;
    }

    if (notificationData.getMessage() == null) {
      log.error("NotificationData.message es null, no se puede enviar notificacion");
      return;
    }

    try {
      String json = JsonUtils.toJson(notificationData);
      String eventId = notificationData.getMessage().getEventId();
      
      kafkaTemplate.send(notificationsTopic, eventId, json)
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.error("Error publicando notificacion a Kafka - EventId: {}: {}", 
                  eventId, ex.getMessage());
            } else {
              log.debug("Notificacion publicada al topic {} - EventId: {}", 
                  notificationsTopic, eventId);
            }
          });
    } catch (Exception e) {
      log.error("Error serializando NotificationData - EventId: {}: {}", 
          notificationData.getMessage().getEventId(), e.getMessage(), e);
    }
  }
}
