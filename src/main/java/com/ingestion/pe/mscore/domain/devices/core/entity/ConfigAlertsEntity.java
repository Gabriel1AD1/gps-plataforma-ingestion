package com.ingestion.pe.mscore.domain.devices.core.entity;

import com.ingestion.pe.mscore.commons.libs.EngineJexl;
import com.ingestion.pe.mscore.commons.models.enums.SeverityStatus;
import com.ingestion.pe.mscore.domain.devices.core.converter.SetConfigAlertNotificationExternalConverter;
import com.ingestion.pe.mscore.domain.devices.core.converter.SetConfigAlertNotificationInternalConverter;
import com.ingestion.pe.mscore.domain.devices.core.models.ConfigAlertNotificationExternal;
import com.ingestion.pe.mscore.domain.devices.core.models.ConfigAlertNotificationInternal;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.*;

@Entity
@Table(name = "config_alerts", schema = "devices_module")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigAlertsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "jexl_script", nullable = false, columnDefinition = "text")
    private String jexlScript;

    @Column(name = "title", nullable = false)
    @Comment("Título de la configuración de alerta")
    private String title;

    @Column(name = "company_id")
    @Comment("Empresa a la que pertenece la tarea programada")
    private Long companyId;

    @Comment("Descripción de la configuración de alerta")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_status", nullable = false)
    @Comment("Nivel de severidad de la alerta")
    private SeverityStatus severityStatus;

    @Comment("Número de veces que el jexl script debe evaluarse como verdadero para activar la alerta")
    @ColumnDefault("'1'")
    @Column(name = "count_event_for_activate")
    private Long countEventForActivate;

    @Comment("Número de veces que el jexl script debe evaluarse como falso para desactivar la alerta")
    @ColumnDefault("'1'")
    @Column(name = "count_event_for_deactivate")
    private Long countEventForDeactivate;

    @Column(name = "max_devices_used", nullable = false)
    @Comment("Número máximo de dispositivos que pueden usar esta configuración")
    private Long maxDevicesUsed;

    @CreationTimestamp
    private Instant created;

    @UpdateTimestamp
    private Instant updated;

    @Convert(converter = SetConfigAlertNotificationInternalConverter.class)
    @Column(name = "notification_internals", columnDefinition = "json")
    @Comment("Notificaciones internas en formato JSON")
    @ColumnTransformer(write = "?::json")
    private Set<ConfigAlertNotificationInternal> notificationInternals;

    @Convert(converter = SetConfigAlertNotificationExternalConverter.class)
    @Column(name = "notification_externals", columnDefinition = "json")
    @Comment("Notificaciones externas en formato JSON")
    @ColumnTransformer(write = "?::json")
    private Set<ConfigAlertNotificationExternal> notificationExternals;

    public boolean evaluateJexlScript(Map<String, Object> attributes) {
        return EngineJexl.evaluateBooleanExpression(this.jexlScript, attributes);
    }

    public void addNotificationExternal(Set<ConfigAlertNotificationExternal> notificationExternals) {
        this.notificationExternals.addAll(notificationExternals);
    }

    public void removeNotificationExternal(Set<ConfigAlertNotificationExternal> notificationExternals) {
        this.notificationExternals.removeAll(notificationExternals);
    }

    public void addNotificationInternal(Set<ConfigAlertNotificationInternal> notificationInternals) {
        this.notificationInternals.addAll(notificationInternals);
    }

    public void removeNotificationInternal(Set<ConfigAlertNotificationInternal> notificationInternals) {
        this.notificationInternals.removeAll(notificationInternals);
    }
}
