package com.ingestion.pe.mscore.bridge.pub.service;

import com.ingestion.pe.mscore.bridge.pub.models.GeofenceEventDto;
import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaBusinessEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.events.geofence:events.geofence}")
    private String geofenceEventTopic;

    public void publishGeofenceEvent(GeofenceEventDto event) {
        try {
            String payload = JsonUtils.toJson(event);
            kafkaTemplate.send(geofenceEventTopic, payload);
            log.debug("Published geofence business event to topic {}: {}", geofenceEventTopic, payload);
        } catch (Exception e) {
            log.error("Error publishing geofence event: {}", e.getMessage(), e);
        }
    }
}
