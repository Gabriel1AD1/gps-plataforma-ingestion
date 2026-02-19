package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static com.ingestion.pe.mscore.domain.devices.app.factory.DeviceWebsocketMessageRefreshFactory.newDeviceStatus;
import static com.ingestion.pe.mscore.domain.devices.app.factory.DeviceWebsocketMessageRefreshFactory.newSummaryDevice;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.devices.app.handlers.models.StatusDevice;
import com.ingestion.pe.mscore.domain.devices.core.dto.response.DevicesStatusSummary;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;

import java.util.List;

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
                            // Stubbed out summary message for now as repo method not fully migrated
                            // WebsocketMessage summaryMessage = sendNewSummary(device.getCompany());
                            // kafkaPublisherService.publishWebsocketMessage(summaryMessage);
                        });
            }
        } catch (Exception e) {
            log.warn("Ocurrió un error al actualizar el estado del dispositivo: {}", e.getMessage());
        }
    }

    private static WebsocketMessage getWebsocketMessageStatus(DeviceEntity device) {
        return newDeviceStatus(device);
    }

    /**
     * @param companyId Empresa a la cual se debe notificar el resumen
     */
    protected WebsocketMessage sendNewSummary(Long companyId) {
        // List<DevicesStatusSummary> summaryStatusSystems =
        // deviceEntityRepository.allSummary(companyId);
        // return newSummaryDevice(companyId, summaryStatusSystems);
        return null;
    }
}
