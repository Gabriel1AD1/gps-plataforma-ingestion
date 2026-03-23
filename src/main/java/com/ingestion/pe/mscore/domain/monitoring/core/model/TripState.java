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
public class TripState implements Serializable {

    // Identificadores
    private Long tripId;
    private Long routeId;
    private Long vehicleId;
    private Long driverId;
    private String direction;

    // Progreso en la ruta
    private int currentPointIndex; // Último control point cruzado (índice en la lista)
    private Double accumulatedDistanceKm; // Distancia recorrida desde el inicio

    // Última posición GPS conocida
    private Instant lastUpdateTime;
    private Double lastLatitude;
    private Double lastLongitude;

    // Análisis de retraso
    private Instant dispatchTime;
    private Double accumulatedDelayMinutes;
    private String status; // ON_TIME, DELAYED, EARLY
    private Double lastSpeedKmh;
}
