package com.ingestion.pe.mscore.domain.devices.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "device_config_alerts", schema = "devices_module")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceConfigAlertsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "device_id")
    @JsonIgnore
    private DeviceEntity device;

    @ManyToOne
    @JoinColumn(name = "config_alerts_id")
    private ConfigAlertsEntity configAlerts;

    @Column(name = "is_active", nullable = false)
    @Comment("Indica si la alerta está activa")
    private boolean isActive;

    @Column(name = "current_count_match", nullable = false)
    @Comment("Conteo actual de coincidencias")
    private Long currentCountMatch;

    @Column(name = "last_match_at")
    @Comment("Fecha de la última coincidencia")
    private Instant lastMatchAt;

    @Column(name = "send_event_at")
    @Comment("Fecha del último evento enviado")
    private Instant sendEventAt;

    @CreationTimestamp
    private Instant created;

    @UpdateTimestamp
    private Instant updated;

    @Column(name = "event_id")
    @Comment("ID del evento generado")
    private UUID eventId;

    public void incrementCountMatch() {
        this.currentCountMatch++;
        this.lastMatchAt = Instant.now();
    }

    public void resetCountMatch() {
        this.currentCountMatch = 0L;
    }

    public void activate() {
        this.isActive = true;
        this.eventId = UUID.randomUUID();
    }

    public void markAsResolved() {
        this.isActive = false;
        this.currentCountMatch = 0L;
        this.eventId = null;
    }

    public boolean isReadyToActivate() {
        return this.currentCountMatch >= this.configAlerts.getCountEventForActivate();
    }

    public boolean isReadyToDeactivate() {
        return this.currentCountMatch >= this.configAlerts.getCountEventForDeactivate();
    }
}
