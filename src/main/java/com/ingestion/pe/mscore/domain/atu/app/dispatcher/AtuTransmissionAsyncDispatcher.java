package com.ingestion.pe.mscore.domain.atu.app.dispatcher;

import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.atu.app.service.AtuTransmissionUseCase;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AtuTransmissionAsyncDispatcher {

    private final AtuTransmissionUseCase atuTransmissionUseCase;

    @Async("taskExecutor")
    public void dispatchAsync(Position position, TripState tripState) {
        try {
            atuTransmissionUseCase.evaluateAndTransmit(position, tripState);
        } catch (Exception e) {
            log.error("Error no manejado en dispatcher asíncrono ATU para IMEI={}: {}", position.getImei(), e.getMessage());
        }
    }
}
