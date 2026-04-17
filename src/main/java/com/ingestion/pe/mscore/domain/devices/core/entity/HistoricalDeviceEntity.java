package com.ingestion.pe.mscore.domain.devices.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingestion.pe.mscore.commons.converter.MapConverter;
import com.ingestion.pe.mscore.commons.models.Position;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "historical_device", schema = "devices_module")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalDeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historical_seq")
    @SequenceGenerator(name = "historical_seq", sequenceName = "historical_batch_seq", schema = "devices_module", allocationSize = 100)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trace_uuid")
    private UUID traceUuid;

    @JoinColumn(name = "device_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private DeviceEntity device;

    @JsonProperty("protocol")
    private String protocol;

    @Builder.Default
    private Instant serverTime = Instant.now();

    @Column(name = "device_time")
    private Instant deviceTime;

    @Column(name = "fix_time")
    private Instant fixTime;

    private boolean outdated;

    private boolean valid;

    private double latitude;

    private double longitude;

    @Column(name = "altitude_in_meters")
    private double altitudeInMeters;

    @Column(name = "speed_in_km")
    private double speedInKm;

    private double course;

    private String address;

    private double accuracy;

    @Convert(converter = MapConverter.class)
    @Column(name = "attributes", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Convert(converter = MapConverter.class)
    @Column(name = "sensors_raw", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, Object> sensorsRaw = Map.of();

    public static HistoricalDeviceEntity map(Position position, DeviceEntity device) {
        HistoricalDeviceEntity historicalDeviceEntity = new HistoricalDeviceEntity();

        UUID traceUuid;
        try {
            traceUuid = position.getCorrelationId() != null
                ? UUID.fromString(position.getCorrelationId())
                : UUID.randomUUID();
        } catch (IllegalArgumentException e) {
            traceUuid = UUID.randomUUID();
        }

        historicalDeviceEntity.setTraceUuid(traceUuid);
        historicalDeviceEntity.setProtocol(position.getProtocol());
        historicalDeviceEntity.setDevice(device);
        historicalDeviceEntity
                .setDeviceTime(position.getDeviceTime() != null ? position.getDeviceTime().toInstant() : Instant.now());
        historicalDeviceEntity.setFixTime(position.getFixTime() != null ? position.getFixTime().toInstant()
                : historicalDeviceEntity.getDeviceTime());
        historicalDeviceEntity.setOutdated(position.isOutdated());
        historicalDeviceEntity.setValid(position.isValid());
        historicalDeviceEntity.setLatitude(position.getLatitude());
        historicalDeviceEntity.setLongitude(position.getLongitude());
        historicalDeviceEntity.setAltitudeInMeters(position.getAltitude());
        historicalDeviceEntity.setSpeedInKm(position.getSpeedInKm());
        historicalDeviceEntity.setCourse(position.getCourse());
        historicalDeviceEntity.setAddress(position.getAddress());
        historicalDeviceEntity.setAccuracy(position.getAccuracy());
        historicalDeviceEntity.setSensorsRaw(position.getAttributes());
        historicalDeviceEntity.setAttributes(device.getSensor());
        return historicalDeviceEntity;
    }

    public Long getDeviceId() {
        return this.device != null ? this.device.getId() : 0L;
    }

    public enum SortField {
        ID("id"),
        TRACE_UUID("traceUuid"),
        PROTOCOL("protocol"),
        SERVER_TIME("serverTime"),
        DEVICE_TIME("deviceTime"),
        FIX_TIME("fixTime"),
        OUTDATED("outdated"),
        VALID("valid"),
        LATITUDE("latitude"),
        LONGITUDE("longitude"),
        ALTITUDE_IN_METERS("altitudeInMeters"),
        SPEED_IN_KM("speedInKm"),
        COURSE("course"),
        ADDRESS("address"),
        ACCURACY("accuracy");

        private final String fieldName;

        SortField(String fieldName) {
            this.fieldName = fieldName;
        }

        public String field() {
            return fieldName;
        }

        public static SortField from(String value) {
            if (value == null) {
                return SERVER_TIME;
            }
            try {
                return SortField.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return SERVER_TIME;
            }
        }
    }
}
