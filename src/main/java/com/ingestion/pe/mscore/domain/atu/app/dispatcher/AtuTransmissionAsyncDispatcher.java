package com.ingestion.pe.mscore.domain.atu.app.dispatcher;

import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.atu.app.service.AtuTransmissionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Dispatcher asíncrono
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AtuTransmissionAsyncDispatcher {

    private final AtuTransmissionUseCase atuTransmissionUseCase;

    @Async("asyncExecutor")
    public void dispatchAsync(Position position, Long deviceId, Long companyId) {
        try {
            atuTransmissionUseCase.evaluateAndTransmit(position, deviceId, companyId);
        } catch (Exception e) {
            log.error("Error no manejado en dispatcher asíncrono ATU para deviceId={}: {}", deviceId, e.getMessage());
        }
    }
}
