package com.ingestion.pe.mscore.config.kafka.sub;

import com.ingestion.pe.mscore.config.log.LogManager;
import com.ingestion.pe.mscore.domain.devices.app.handlers.DeviceServiceHandler;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.vehicles.app.service.VehicleTrackingFacadeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaSubscriptionPositions {
    private static final Logger log = LoggerFactory.getLogger(KafkaSubscriptionPositions.class);

    private final DeviceServiceHandler deviceServiceHandler;
    private final VehicleTrackingFacadeService vehicleTrackingFacadeService;

    @KafkaListener(topics = "${kafka.topic.position}", groupId = "${kafka.group.id}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(String positionObj) {
        try {
            log.info("Position received: {}", positionObj);

            Position position = Position.fromJson(positionObj);
            log.debug("Posición parseada: {}", position);
            log.info("🔍 [DEBUG] RAW Parsing IMEI: '{}'", position.getImei()); // Log exacto del IMEI parseado
            LogManager.addCorrelationId(position.getCorrelationId());
            log.debug("Agregado CorrelationId al LogManager: {}", position.getCorrelationId());
            try {
                deviceServiceHandler.handleDeviceEvent(position);
            } catch (Exception e) {
                log.error("Error en el procesamiento del dispositivo para la posición: {}", e.getMessage(), e);
            }
            try {
                vehicleTrackingFacadeService.processPositionForTracking(position);
            } catch (Exception e) {
                log.error("Error en el procesamiento de tracking para la posición: {}", e.getMessage(), e);
            }

            log.debug("Position processed (core + tracking): IMEI={}", position.getImei());

        } catch (Exception e) {
            log.error("Error processing position: {}", e.getMessage(), e);
        }
    }
}
