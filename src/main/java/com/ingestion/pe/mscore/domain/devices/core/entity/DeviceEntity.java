package com.ingestion.pe.mscore.domain.devices.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingestion.pe.mscore.commons.converter.JsonbListMapConverter;
import com.ingestion.pe.mscore.commons.converter.MapConverter;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.commons.models.enums.EntityStatus;
import com.ingestion.pe.mscore.domain.devices.core.converter.DeviceStatusConverter;
import com.ingestion.pe.mscore.domain.devices.core.converter.DeviceTypeConverter;
import com.ingestion.pe.mscore.domain.devices.core.converter.SensorModelConverter;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceStatus;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceType;
import com.ingestion.pe.mscore.domain.devices.core.models.SensorModel;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "devices", schema = "devices_module")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEntity {

    private static final Logger log = LoggerFactory.getLogger(DeviceEntity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @Comment("UUID externo del dispositivo")
    private String imei;

    @Column(nullable = false, unique = true, length = 50)
    @Comment("Número de serie del dispositivo")
    private String serialNumber;

    @Column(name = "speed_in_km")
    @Comment("Velocidad en nudos")
    @ColumnDefault("0")
    private Double speedInKmh;

    @Convert(converter = DeviceTypeConverter.class)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(nullable = false)
    @Comment("Contraseña del dispositivo")
    private String password;

    @Column(name = "firmware_version", length = 50, nullable = false)
    private String firmwareVersion;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    @ColumnDefault("0")
    @Comment("Odómetro del dispositivo en metros")
    private Long odometerInMeters;

    @Column(nullable = false, name = "last_connection")
    @Comment("Ultima connexión del dispositivo")
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant lastConnection;

    @Column(nullable = false, name = "last_disconnection")
    @Comment("Ultima desconexión del dispositivo")
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant lastDisconnection;

    @Column(nullable = false, name = "last_data_received")
    @Comment("Último dato recibido del dispositivo")
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant lastDataReceived;

    @Column(nullable = false, length = 50)
    private String brand;

    @CreationTimestamp
    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "company_id", nullable = false)
    @Comment("ID de la empresa propietaria del dispositivo")
    @JsonIgnore
    private Long company;

    @Convert(converter = DeviceStatusConverter.class)
    @Column(name = "device_status", nullable = false)
    @Comment("Estado del dispositivo online - offline - unknown")
    private DeviceStatus deviceStatus;

    @Column
    @Comment("Número de satélites conectados al dispositivo")
    @ColumnDefault("'0.0'")
    private Double satellite;

    @Column(name = "horizontal_dilution_of_precision", nullable = false)
    @Comment("Dilución horizontal de precisión del dispositivo")
    @ColumnDefault("'0.0'")
    private Double horizontalDilutionOfPrecision;

    @Column(nullable = false)
    @Comment("Precisión del dispositivo")
    @ColumnDefault("'0.0'")
    private Double accuracy;

    @Column(nullable = false, length = 50)
    @Comment("Dirección del dispositivo")
    @ColumnDefault("'0.0'")
    private Double course;

    @Column(nullable = false)
    @Comment("Altitud del dispositivo")
    @ColumnDefault("'0.0'")
    private Double altitude;

    @Column(nullable = false, length = 50)
    @Comment("Nivel de batería del dispositivo")
    @ColumnDefault("'0.0'")
    private Double battery;

    @Column(name = "latitude", nullable = false)
    @Comment("Latitud del dispositivo")
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    @Comment("Longitud del dispositivo")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Comment("Estado del dispositivo")
    private EntityStatus status;

    @Column(name = "sensor", columnDefinition = "TEXT")
    @Convert(converter = MapConverter.class)
    @Comment("Datos de sensores del dispositivo en formato JSON ya procesados por el jexl script")
    private Map<String, Object> sensor;

    @Column(name = "sensor_raw", columnDefinition = "TEXT")
    @Convert(converter = MapConverter.class)
    private Map<String, Object> sensorRaw;

    @Column(name = "properties", length = 4000)
    @Convert(converter = MapConverter.class)
    private Map<String, Object> properties;

    @Column(name = "last_data_history", columnDefinition = "json")
    @Convert(converter = JsonbListMapConverter.class)
    @ColumnTransformer(write = "?::json")
    @Comment("Historial 30 ultimos datos del dispositivo de manera cruda en formato JSON")
    private List<Map<String, Object>> dataHistory;
    /*
     * @ManyToOne(fetch = FetchType.LAZY)
     * 
     * @JoinColumn(name = "override_sensors_id")
     * 
     * @Comment("Configuración de sobreescritura de sensores asociada al dispositivo"
     * )
     * private OverrideSensorsEntity overrideSensors;
     */
    @Column(name = "last_historical_device_id")
    @Comment("Último registro histórico asociado al dispositivo")
    private Long lastHistoricalDevice;

    @Column(name = "sensors_data", columnDefinition = "TEXT")
    @Convert(converter = SensorModelConverter.class)
    @Comment("Lista de sensores del dispositivo")
    private Set<SensorModel> sensorsData;

    @Column(name = "sensor_on_time", columnDefinition = "TEXT")
    @Convert(converter = MapConverter.class)
    @Comment("Datos de sensores del dispositivo en formato con los datos de los sensores con tiempo y todo")
    private Map<String, Object> sensorOnTime;

    @UpdateTimestamp
    @Column(name = "updated", nullable = false)
    private Instant updated;

    @Column(name = "device_time", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant deviceTime;

    public static DeviceEntity of(Long deviceId) {
        return DeviceEntity.builder().id(deviceId).build();
    }

    @PrePersist
    public void prePersist() {
        this.horizontalDilutionOfPrecision = Objects.requireNonNullElse(this.horizontalDilutionOfPrecision, 0.0);
        this.satellite = Objects.requireNonNullElse(this.satellite, 0.0);
        this.deviceTime = Instant.now();
        this.dataHistory = new ArrayList<>();
        this.sensorsData = new HashSet<>();
        this.accuracy = Objects.requireNonNullElse(this.accuracy, 0.0);
        this.course = Objects.requireNonNullElse(this.course, 0.0);
        this.altitude = Objects.requireNonNullElse(this.altitude, 0.0);
        this.battery = Objects.requireNonNullElse(this.battery, 0.0);
        this.latitude = Objects.requireNonNullElse(this.latitude, 0.0);
        this.longitude = Objects.requireNonNullElse(this.longitude, 0.0);
        this.sensor = new HashMap<>();
        this.sensorRaw = Map.of();
        this.sensorOnTime = new HashMap<>();
        this.speedInKmh = Objects.requireNonNullElse(this.speedInKmh, 0.0);
        this.status = EntityStatus.ACTIVE;
        this.deviceStatus = DeviceStatus.offline;
        this.imei = Objects.requireNonNullElse(this.imei, "unknown-imei");
        this.lastConnection = Instant.now();
        this.lastDisconnection = Instant.now();
        this.lastDataReceived = Instant.now();
        this.odometerInMeters = Objects.requireNonNullElse(this.odometerInMeters, 0L);
        this.created = Instant.now();
        this.updated = Instant.now();
        this.properties = Objects.requireNonNullElse(this.properties, new HashMap<>());
    }

    public List<Map<String, Object>> getSensorDataMap() {
        return Optional.ofNullable(this.sensorsData).orElse(Set.of()).stream()
                .map(SensorModel::toMap)
                .toList();
    }

    public void addDataHistory(Map<String, Object> newData) {
        Map<String, Object> attributes = new HashMap<>(newData);
        // Agregar nuevo dato al historial de datos
        log.debug("Agregar nuevo dato al historial de datos: {}", newData);

        // Inicializar los objetos en caso estos sean nulos o vacios

        // inicializar los sensores crudos
        this.sensorRaw = attributes;
        updateSensorData(attributes);
        this.sensorOnTime = getSensorOnTime(this.sensorsData);

        log.debug("Sensor raw actualizado: {}", this.sensorRaw);
        // Aplicar la sobreescritura de sensores si existe una configuración
        log.debug("Sensor data recibido de bd antes de sobreescritura: {}", sensorsData);
        var sensorsLast = new HashMap<>(this.sensorOnTime);

        this.sensor = cleanSensor(sensorsLast);
        /*
         * Optional.ofNullable(this.overrideSensors)
         * .ifPresent(
         * override -> {
         * log.debug("Aplicando sensores data {}", sensorsData);
         * // newData (sensorRaw) es la fuente de verdad
         * log.debug("Sensores combinados para sobreescritura: {}", sensorsLast);
         * 
         * // Ejecutar el script JEXL para obtener los sensores sobreescritos
         * // Actualizar el mapa de sensores con los valores sobreescritos
         * // No incluir los tiempos en el sensor con tiempo
         * Map<String, Object> executeSensor = override.executeJexlScript(sensorsLast);
         * Map<String, Object> cleanSensor = cleanSensor(executeSensor);
         * this.sensor = cleanSensor;
         * updateSensorData(cleanSensor);
         * this.sensorOnTime = getSensorOnTime(this.sensorsData);
         * });
         */

        if (this.dataHistory.size() >= 50) {
            // elimina el primero (el más antiguo)
            this.dataHistory.remove(0);
        }
        dataHistory.add(sensor);

        log.debug("Conjunto de sensores actualizado:  {}", this.sensorsData);
    }

    private void updateSensorData(Map<String, Object> attributes) {
        attributes.forEach(
                (key, value) -> {
                    log.debug("Sobreescritura del sensor {}: {}", key, value);
                    Optional<SensorModel> existing = this.sensorsData.stream().filter(s -> s.getKey().equals(key))
                            .findFirst();

                    if (existing.isPresent()) {
                        log.debug("Sobreescritura del sensor {}: {}", key, existing.get());
                        existing.get().updateValue(String.valueOf(value));
                    } else {
                        if (!key.endsWith("_in_seconds")) {
                            log.debug("Se agrega nuevo sensor {}: {}", key, value);
                            this.sensorsData.add(SensorModel.of(key, String.valueOf(value), Instant.now()));
                        }
                    }
                    log.debug("Sensor Data actualizado por cada iteracion {}", this.sensorsData);
                });
    }

    private Map<String, Object> cleanSensor(Map<String, Object> sensor) {
        return sensor.entrySet().stream()
                .filter(entry -> !entry.getKey().endsWith("_in_seconds"))
                .collect(
                        HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll);
    }

    public HashMap<String, Object> getSensorOnTime(Set<SensorModel> sensorsData) {
        HashMap<String, Object> sensorMap = new HashMap<>();
        if (sensorsData != null) {
            for (SensorModel sensor : sensorsData) {
                sensorMap.put(sensor.getKey(), sensor.getValue());
                sensorMap.put(sensor.getKey() + "_in_seconds", sensor.getTimeInCurrentStateSeconds());
            }
        }
        return sensorMap;
    }

    public void updateNewConnection() {
        this.deviceStatus = DeviceStatus.online;
        this.lastConnection = Instant.now();
    }

    public void updateNewDisconnection() {
        this.deviceStatus = DeviceStatus.offline;
        this.lastDisconnection = Instant.now();
    }

    public void updateLastDataReceived() {
        this.deviceStatus = DeviceStatus.online;
        this.lastDataReceived = Instant.now();
    }

    /**
     * @param newLatitude  nueva latitud del dispositivo
     * @param newLongitude nueva longitud del dispositivo
     */
    public void calculateNewOdometer(Double newLatitude, Double newLongitude) {
        if (this.latitude != null && this.longitude != null) {
            double earthRadius = 6371e3;
            double dLat = Math.toRadians(newLatitude - this.latitude);
            double dLon = Math.toRadians(newLongitude - this.longitude);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(this.latitude))
                            * Math.cos(Math.toRadians(newLatitude))
                            * Math.sin(dLon / 2)
                            * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = earthRadius * c;

            this.odometerInMeters += (long) distance;
        }
        this.latitude = newLatitude;
        this.longitude = newLongitude;
    }

    public void handlerPosition(Position position) {
        log.debug("Iniciando el manejo de la posición para el dispositivo con IMEI: {}", this.imei);
        addDataHistory(position.getAttributes());
        calculateNewOdometer(position.getLatitude(), position.getLongitude());
        this.deviceTime = position.getDeviceTime().toInstant();
        this.speedInKmh = position.getSpeedInKm();
        this.latitude = position.getLatitude();
        this.longitude = position.getLongitude();
        this.accuracy = position.getAccuracy();
        this.course = position.getCourse();
        this.altitude = position.getAltitude();
        this.deviceStatus = DeviceStatus.online;
        this.battery = position.getBattery();
        this.satellite = position.getSatellites();
        this.horizontalDilutionOfPrecision = position.getHorizontalDilutionOfPrecision();
        updateLastDataReceived();
    }
}
