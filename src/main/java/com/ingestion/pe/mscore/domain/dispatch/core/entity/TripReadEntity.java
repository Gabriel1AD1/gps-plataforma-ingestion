package com.ingestion.pe.mscore.domain.dispatch.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trip", schema = "dispatch_module")
public class TripReadEntity {

    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "route_id", nullable = false, insertable = false, updatable = false)
    private Long routeId;

    @Column(name = "vehicle_id", nullable = false, insertable = false, updatable = false)
    private Long vehicleId;

    @Column(name = "company_id", nullable = false, insertable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false, insertable = false, updatable = false)
    private String status;

    @Column(nullable = false, insertable = false, updatable = false)
    private String direction;

    @Column(name = "lap_number", nullable = false, insertable = false, updatable = false)
    private Integer lapNumber;

    @Column(name = "dispatch_time", nullable = false, insertable = false, updatable = false)
    private Instant dispatchTime;

    @Column(name = "departure_terminal_id", insertable = false, updatable = false)
    private Long departureTerminalId;

    @Column(name = "arrival_terminal_id", insertable = false, updatable = false)
    private Long arrivalTerminalId;
}
