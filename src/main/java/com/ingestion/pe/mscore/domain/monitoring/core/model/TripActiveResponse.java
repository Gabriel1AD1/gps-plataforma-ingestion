package com.ingestion.pe.mscore.domain.monitoring.core.model;

import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripActiveResponse implements Serializable {
    // Deserializado desde Redis key "trip:active:{tripId}"
    private Long id;
    private Long routeId;
    private Long vehicleId;
    private Long driverId;
    private Long companyId;
    private String direction; 
    private Integer lapNumber;
    private String status; 
    private Instant dispatchTime;
    private Long departureTerminalId;
    private Long arrivalTerminalId;
}
