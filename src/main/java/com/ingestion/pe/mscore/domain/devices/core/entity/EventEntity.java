package com.ingestion.pe.mscore.domain.devices.core.entity;

import com.ingestion.pe.mscore.bridge.pub.models.ApplicationEventCreate;
import com.ingestion.pe.mscore.commons.converter.JsonSetStringConverter;
import com.ingestion.pe.mscore.commons.converter.MapConverter;
import com.ingestion.pe.mscore.commons.converter.SetNotificationExternalConverter;
import com.ingestion.pe.mscore.commons.converter.SetNotificationInternalConverter;
import com.ingestion.pe.mscore.commons.models.enums.SeverityStatus;
import com.ingestion.pe.mscore.commons.models.enums.NotificationTypes;
import com.ingestion.pe.mscore.commons.models.events.EventNotificationExternal;
import com.ingestion.pe.mscore.commons.models.events.EventNotificationInternal;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "events", schema = "events_module")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "event_id", nullable = false, unique = true, length = 36)
  private String eventId; 

  @Convert(converter = JsonSetStringConverter.class)
  @Column(name = "user_exclude_ids")
  private Set<String> userExcludeIds;

  @Column(name = "event_type", nullable = false)
  private String eventType; 

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType; 

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT", nullable = false)
  private String description;

  @Convert(converter = MapConverter.class)
  @Column(name = "properties", columnDefinition = "json")
  @ColumnTransformer(write = "?::json")
  private Map<String, Object> properties;

  @Column(name = "application_name", nullable = false)
  private String applicationName; 

  @Column(name = "severity_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private SeverityStatus status;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "module_name", nullable = false)
  private String moduleName; 

  @Column(name = "resolved", nullable = false)
  private Boolean resolved;

  @Column(name = "resolution_notes", columnDefinition = "TEXT")
  private String resolutionNotes;

  @Convert(converter = MapConverter.class)
  @Column(name = "resolution_properties", length = 4000)
  @ColumnDefault("'{}'")
  private Map<String, Object> resolutionProperties;

  @Column(name = "active", nullable = false)
  private Boolean active;

  @Column(name = "resolved_time")
  private Instant resolvedTime;

  @Column(name = "correlation_id")
  private String correlationId;

  @Column(name = "deleted", nullable = false)
  private Boolean deleted;

  @Column(name = "user_id_deleted")
  private Long userIdDeleted;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @Convert(converter = SetNotificationExternalConverter.class)
  @Column(name = "notification_externals", columnDefinition = "json")
  @ColumnTransformer(write = "?::json")
  private Set<EventNotificationExternal> notificationExternals;

  @Convert(converter = SetNotificationInternalConverter.class)
  @Column(name = "notification_internals", columnDefinition = "json")
  @ColumnTransformer(write = "?::json")
  private Set<EventNotificationInternal> notificationInternals;

  @Column(name = "latitude")
  @ColumnDefault("'0.0'")
  private Double latitude;

  @Column(name = "longitude")
  @ColumnDefault("'0.0'")
  private Double longitude;

  public void markIsResolved(String resolutionNotes, Map<String, Object> resolutionProperties) {
    this.resolved = true;
    this.active = false;
    this.resolutionNotes = resolutionNotes;
    this.resolutionProperties = resolutionProperties;
    this.resolvedTime = Instant.now();
  }

  @PrePersist
  public void prePersist() {
    this.resolutionProperties = Objects.requireNonNullElse(resolutionProperties, Map.of());
    this.latitude = Objects.requireNonNullElse(latitude, Double.NaN);
    this.longitude = Objects.requireNonNullElse(longitude, Double.NaN);
    notificationExternals = Objects.requireNonNullElse(notificationExternals, Set.of());
    notificationInternals = Objects.requireNonNullElse(notificationInternals, Set.of());
    this.userIdDeleted = null;
    this.deletedAt = null;
    this.userExcludeIds = Objects.requireNonNullElse(this.userExcludeIds, Set.of());
  }

  public static EventEntity map(ApplicationEventCreate applicationEventCreate) {
    Set<EventNotificationExternal> notificationExternalsMap =
        applicationEventCreate.getNotificationExternals().stream()
            .map(
                ne ->
                    EventNotificationExternal.builder()
                        .notificationType(NotificationTypes.valueOf(ne.getNotificationType().name()))
                        .destinations(ne.getDestination())
                        .build())
            .collect(Collectors.toSet());
    Set<EventNotificationInternal> notificationInternalsMap =
        applicationEventCreate.getNotificationInternals().stream()
            .map(
                ni ->
                    EventNotificationInternal.builder()
                        .userId(ni.getUserId())
                        .notificationTypes(ni.getNotificationTypes().stream()
                                .map(nt -> NotificationTypes.valueOf(nt.name()))
                                .collect(Collectors.toSet()))
                        .build())
            .collect(Collectors.toSet());

    return EventEntity.builder()
        .eventId(applicationEventCreate.getEventId())
        .eventType(applicationEventCreate.getEventType().name())
        .occurredAt(applicationEventCreate.getOccurredAt())
        .aggregateType(applicationEventCreate.getAggregateType())
        .aggregateId(applicationEventCreate.getAggregateId())
        .companyId(applicationEventCreate.getCompanyId())
        .notificationExternals(notificationExternalsMap)
        .notificationInternals(notificationInternalsMap)
        .title(applicationEventCreate.getTitle())
        .status(applicationEventCreate.getSeverityStatus())
        .description(applicationEventCreate.getDescription())
        .properties(applicationEventCreate.getProperties())
        .applicationName(applicationEventCreate.getApplicationName().name())
        .moduleName(applicationEventCreate.getModuleName().name())
        .resolved(applicationEventCreate.getResolved())
        .resolvedTime(applicationEventCreate.getResolvedTime())
        .resolutionProperties(Map.of())
        .latitude(applicationEventCreate.getLatitude())
        .longitude(applicationEventCreate.getLongitude())
        .active(applicationEventCreate.getActive())
        .deleted(false)
        .correlationId(
            applicationEventCreate.getCorrelationId() != null
                ? applicationEventCreate.getCorrelationId()
                : "null")
        .build();
  }
}
