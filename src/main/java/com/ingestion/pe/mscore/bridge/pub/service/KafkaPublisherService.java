package com.ingestion.pe.mscore.bridge.pub.service;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.websocket}")
    private String websocketTopic;

    @Value("${kafka.topic.notifications}")
    private String notificationsTopic;

    public void publishWebsocketMessage(WebsocketMessage message) {
        try {
            String payload = JsonUtils.toJson(message);
            kafkaTemplate.send(websocketTopic, payload);
            log.debug("Mensaje websocket publicado a topic {}: {}", websocketTopic, payload);
        } catch (Exception e) {
            log.error("Error publicando mensaje websocket: {}", e.getMessage(), e);
        }
    }

//     public void publishNotificationEvent(Object event) {
//         try {
//             String payload = JsonUtils.toJson(event);
//             kafkaTemplate.send(notificationsTopic, payload);
//             log.info("Evento de notificación publicado a topic '{}': {}", notificationsTopic, payload);
//         } catch (Exception e) {
//             log.error("Error publicando evento de notificación a Kafka: {}", e.getMessage(), e);
//         }
//     }
}

