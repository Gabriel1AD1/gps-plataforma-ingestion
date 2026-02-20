package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static com.ingestion.pe.mscore.domain.devices.app.factory.DeviceWebsocketMessageRefreshFactory.newDeviceStatus;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.devices.app.handlers.models.StatusDevice;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusDeviceServiceHandler {
    private static final Logger log = LoggerFactory.getLogger(StatusDeviceServiceHandler.class);
    private final DeviceEntityRepository deviceEntityRepository;
    private final KafkaPublisherService kafkaPublisherService;

    public void handleStatusDeviceService(StatusDevice message) {
        try {
            if (message.getResetDevices()) {
                deviceEntityRepository.updateAllStatuses(message.getStatus());
            } else {
                var deviceEntity = deviceEntityRepository.findById(message.getDeviceId());
                deviceEntity.ifPresent(
                        device -> {
                            device.setDeviceStatus(message.getStatus());
                            switch (message.getStatus()) {
                                case online -> device.updateNewConnection();
                                case offline, unknown -> device.updateNewDisconnection();
                            }
                            deviceEntityRepository.save(device);
                            var statusMessage = getWebsocketMessageStatus(device);
                            kafkaPublisherService.publishWebsocketMessage(statusMessage);

                        });
            }
        } catch (Exception e) {
            log.warn("Ocurrió un error al actualizar el estado del dispositivo: {}", e.getMessage());
        }
    }

    private static WebsocketMessage getWebsocketMessageStatus(DeviceEntity device) {
        return newDeviceStatus(device);
    }

    protected WebsocketMessage sendNewSummary(Long companyId) {

        return null;
    }
}
