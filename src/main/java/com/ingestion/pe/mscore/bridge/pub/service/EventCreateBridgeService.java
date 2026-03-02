package com.ingestion.pe.mscore.bridge.pub.service;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventCreateBridgeService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.events:events}")
    private String eventsTopic;

    public void createEvent(Object event) {
        try {
            String payload = JsonUtils.toJson(event);
            kafkaTemplate.send(eventsTopic, payload);
            log.info("Evento publicado aKafka topic '{}': {}", eventsTopic, payload);
        } catch (Exception e) {
            log.error("Falló al publicar evento a Kafka: {}", e.getMessage(), e);
        }
    }
}
