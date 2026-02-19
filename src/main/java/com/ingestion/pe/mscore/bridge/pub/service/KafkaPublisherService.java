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

    @Value("${kafka.topic.websocket-push:websocket.push}")
    private String websocketTopic;

    public void publishWebsocketMessage(WebsocketMessage message) {
        try {
            String payload = JsonUtils.toJson(message);
            kafkaTemplate.send(websocketTopic, payload);
            log.debug("Published websocket message to topic {}: {}", websocketTopic, payload);
        } catch (Exception e) {
            log.error("Error publishing websocket message: {}", e.getMessage(), e);
        }
    }
}
