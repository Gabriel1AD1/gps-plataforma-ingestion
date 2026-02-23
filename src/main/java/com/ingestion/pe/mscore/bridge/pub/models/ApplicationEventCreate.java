package com.ingestion.pe.mscore.bridge.pub.models;

import com.ingestion.pe.mscore.bridge.pub.models.enums.ApplicationName;
import com.ingestion.pe.mscore.bridge.pub.models.enums.ModuleName;
import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.commons.models.enums.EventTypeEnumerated;
import com.ingestion.pe.mscore.commons.models.enums.SeverityStatus;
import com.ingestion.pe.mscore.domain.auth.core.enums.NotificationTypes;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationEventCreate {
    private String eventId;
    private Set<String> userExcludeIds;
    private EventTypeEnumerated eventType;
    private Instant occurredAt;
    private String aggregateType;
    private String aggregateId;
    private SeverityStatus severityStatus;
    private String title;
    private String description;
    private Map<String, Object> properties;
    private Long companyId;
    private ApplicationName applicationName;
    private ModuleName moduleName;
    private String resolutionNotes;

    private Boolean resolved;
    private Instant resolvedTime;

    private Boolean active;

    private String correlationId;

    private Set<NotificationInternal> notificationInternals;
    private Set<NotificationExternal> notificationExternals;

    private Double latitude;
    private Double longitude;

    public static ApplicationEventCreate fromJson(String eventObj) {
        return JsonUtils.toObject(eventObj, ApplicationEventCreate.class);
    }

    public static ApplicationEventCreateBuilder builder() {
        return new ApplicationEventCreateBuilder()
                .latitude(Double.NaN)
                .longitude(Double.NaN)
                .correlationId(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .properties(Map.of())
                .notificationExternals(Set.of())
                .notificationInternals(Set.of())
                .aggregateType("Unknown")
                .applicationName(ApplicationName.ms_core)
                .moduleName(ModuleName.unknown)
                .resolutionNotes("")
                .resolvedTime(null)
                .eventType(EventTypeEnumerated.UNKNOWN_EVENT)
                .severityStatus(SeverityStatus.LOW)
                .resolved(false)
                .active(true);
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationInternal {
        private Set<NotificationTypes> notificationTypes;
        private Long userId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationExternal {
        private NotificationTypes notificationType;
        private Set<String> destination;
    }
}
