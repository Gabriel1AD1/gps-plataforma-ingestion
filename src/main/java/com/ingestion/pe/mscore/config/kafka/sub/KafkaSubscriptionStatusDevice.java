package com.ingestion.pe.mscore.config.kafka.sub;

import com.ingestion.pe.mscore.config.log.LogManager;
import com.ingestion.pe.mscore.domain.devices.app.handlers.StatusDeviceServiceHandler;
import com.ingestion.pe.mscore.domain.devices.app.handlers.models.StatusDevice;
import com.ingestion.pe.mscore.domain.vehicles.app.manager.VehicleServiceHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaSubscriptionStatusDevice {
  private static final Logger log = LoggerFactory.getLogger(KafkaSubscriptionStatusDevice.class);

  private final StatusDeviceServiceHandler deviceServiceHandler;
  private final VehicleServiceHandler vehicleServiceHandler;


  @KafkaListener(topics = "${kafka.topic.devices}", groupId = "${kafka.group.id}", containerFactory = "kafkaListenerContainerFactory")
  public void listen(String statusDeviceObs) {
    try {
      log.info("📡 Status dispotivio recibida: {}", statusDeviceObs);
      LogManager.addCorrelationId(UUID.randomUUID().toString());
      StatusDevice statusDevice = StatusDevice.fromJson(statusDeviceObs);
      try {
        deviceServiceHandler.handleStatusDeviceService(statusDevice);
      } catch (Exception e) {
        log.error("Error en el procesamiento del estado del dispositivo: {}", e.getMessage(), e);
      }
      try {
        vehicleServiceHandler.processStatusForVehicle(statusDevice);
      } catch (Exception e) {
        log.error("Error en el procesamiento del estado del dispositivo: {}", e.getMessage(), e);
      }
    } catch (Exception e) {
      log.error("Error al procesar la posición: {}", e.getMessage());
    }
  }
}
