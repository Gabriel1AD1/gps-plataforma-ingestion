package com.ingestion.pe.mscore.config.kafka.sub;

import com.ingestion.pe.mscore.domain.devices.app.handlers.DeviceBatchOrchestrator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaSubscriptionPositions {
    private static final Logger log = LoggerFactory.getLogger(KafkaSubscriptionPositions.class);

    private final DeviceBatchOrchestrator deviceBatchOrchestrator;

    @KafkaListener(topics = "${kafka.topic.position}", groupId = "${kafka.group.id}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(List<String> positions) {
        try {
            log.info("Batch recibido: size={}", positions.size());
            deviceBatchOrchestrator.processBatch(positions);
        } catch (Exception e) {
            log.error("Error procesando batch: {}", e.getMessage(), e);
        }
    }
}
