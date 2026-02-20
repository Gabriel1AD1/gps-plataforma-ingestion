package com.ingestion.pe.mscore.domain.vehicles.app.service.impl;

import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.vehicles.app.service.VehicleTrackingFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingFacadeServiceImpl implements VehicleTrackingFacadeService {

    @Override
    public void processPositionForTracking(Position position) {

        log.info("Processing position for tracking: {}", position.getImei());
    }
}
