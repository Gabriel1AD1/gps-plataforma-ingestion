package com.ingestion.pe.mscore.domain.devices.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "device_config_alert", schema = "devices_module")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "device_id")
    @JsonIgnore
    private DeviceEntity device;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "config_alerts_id")
    private ConfigAlertsEntity configAlerts;

    @Column(name = "active")
    @Comment("Indica si la alerta está activa")
    private Boolean active;

    @Column(name = "event_id")
    @Comment("Id único del evento asociado a la configuración de alerta en formato UUID")
    private String eventId;

    @Column(name = "send_event_at")
    @Comment("Fecha y hora en que se envió el evento de alerta")
    private Instant sendEventAt;

    @Column(name = "activation_date")
    @Comment("Fecha de activación de la configuración de alerta para el dispositivo")
    private Instant activationDate;

    @CreationTimestamp
    private Instant created;

    public void activate() {
        this.active = true;
        this.sendEventAt = Instant.now();
        this.activationDate = Instant.now();
        this.eventId = UUID.randomUUID().toString();
    }

    public void markAsResolved() {
        this.active = false;
        this.activationDate = null;
        this.eventId = "";
        this.sendEventAt = null;
    }

    public boolean isActive() {
        return this.eventId != null && !eventId.isBlank();
    }

    public boolean isInactive() {
        return !Boolean.TRUE.equals(this.active);
    }

    public static DeviceConfigAlertsEntity of(DeviceEntity device, ConfigAlertsEntity configAlerts) {
        return DeviceConfigAlertsEntity.builder()
                .device(device)
                .configAlerts(configAlerts)
                .active(false)
                .sendEventAt(null)
                .eventId("")
                .activationDate(null)
                .build();
    }
}
