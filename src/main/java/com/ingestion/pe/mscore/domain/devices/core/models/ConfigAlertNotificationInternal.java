package com.ingestion.pe.mscore.domain.devices.core.models;

import com.ingestion.pe.mscore.domain.auth.core.enums.NotificationTypes;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.*;

/** Esta clase se usa para notificar usuarios que existen en el sistema */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "userId")
public class ConfigAlertNotificationInternal {
    @NotNull
    private Set<NotificationTypes> notificationTypes;
    @NotNull
    private Long userId;
}
