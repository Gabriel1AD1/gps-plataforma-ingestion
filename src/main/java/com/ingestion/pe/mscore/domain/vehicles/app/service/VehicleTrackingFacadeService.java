package com.ingestion.pe.mscore.domain.vehicles.app.service;

import com.ingestion.pe.mscore.commons.models.Position;

public interface VehicleTrackingFacadeService {
    void processPositionForTracking(Position position);
}
