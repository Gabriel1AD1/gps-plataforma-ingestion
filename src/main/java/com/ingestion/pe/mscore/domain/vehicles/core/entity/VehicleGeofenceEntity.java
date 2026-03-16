package com.ingestion.pe.mscore.domain.vehicles.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.Set;


@Entity
@Table(name = "vehicle_geofence", schema = "vehicle_module")
@Getter
@NoArgsConstructor
public class VehicleGeofenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false, insertable = false, updatable = false)
    private Long vehicleId;

    @Comment("ID del geocerca asociado")
    @Column(name = "geofence_id", nullable = false, insertable = false, updatable = false)
    private Long geofenceId;

    @Comment("Indica si la geocerca está activa para el vehículo")
    @Column(name = "active", nullable = false, insertable = false, updatable = false)
    private Boolean active;

    @Column(name = "notification_internals", columnDefinition = "json",
            insertable = false, updatable = false)
    private String notificationInternalsJson;

    @Column(name = "notification_externals", columnDefinition = "json",
            insertable = false, updatable = false)
    private String notificationExternalsJson;

    @Column(name = "created", insertable = false, updatable = false)
    private Instant created;
}
