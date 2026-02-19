package com.ingestion.pe.mscore.domain.devices.core.models;

import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import com.ingestion.pe.mscore.domain.auth.core.enums.NotificationTypes;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

/** Esta clase se usa para notificar los uisuarios que no estan en el sistema */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "notificationType")
public class ConfigAlertNotificationExternal {
    @NotNull
    private NotificationTypes notificationType;
    @NotNull
    private Set<String> destination;
    @NotNull
    private TimeZone timeZones;
}
