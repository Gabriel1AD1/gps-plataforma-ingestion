package com.ingestion.pe.mscore.domain.atu.app.port;

import java.util.Optional;

public interface DriverDataPort {

    Optional<String> getDriverDocumentNumber(Long driverId);
}
