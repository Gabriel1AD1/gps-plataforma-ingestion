package com.ingestion.pe.mscore.domain.devices.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensorModel {
    private String key;
    private String value;
    private Instant timestamp; // Timestamp de la última actualización de valor
    private Instant lastStateChangeTimestamp; // Timestamp cuando el valor cambió por última vez
    private Long timeInCurrentState; // Tiempo en milisegundos en el estado actual

    public void updateValue(String newValue) {
        Instant now = Instant.now();

        if (!this.value.equals(newValue)) {
            // El valor ha cambiado, reiniciar timeInCurrentState
            this.value = newValue;
            this.timestamp = now;
            this.lastStateChangeTimestamp = now;
            this.timeInCurrentState = 0L;
        } else {
            // El valor es el mismo, actualizar timeInCurrentState
            if (this.lastStateChangeTimestamp == null) {
                this.lastStateChangeTimestamp = now;
            }
            this.timeInCurrentState = now.toEpochMilli() - this.lastStateChangeTimestamp.toEpochMilli();
            this.timestamp = now;
        }
    }

    /** Obtiene el tiempo en el estado actual en segundos */
    @JsonIgnore
    public Long getTimeInCurrentStateSeconds() {
        if (timeInCurrentState == null)
            return 0L;
        return timeInCurrentState / 1000;
    }

    /** Obtiene el tiempo en el estado actual en minutos */
    @JsonIgnore
    public Long getTimeInCurrentStateMinutes() {
        if (timeInCurrentState == null)
            return 0L;
        return timeInCurrentState / (1000 * 60);
    }

    public static SensorModel of(String key, String value, Instant timestamp) {
        return SensorModel.builder()
                .key(key)
                .value(value)
                .timestamp(timestamp)
                .lastStateChangeTimestamp(timestamp)
                .timeInCurrentState(0L)
                .build();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", key);
        map.put("value", value);
        map.put("timestamp", timestamp.toString());
        map.put("lastStateChangeTimestamp", lastStateChangeTimestamp.toString());
        map.put("timeInCurrentState", timeInCurrentState);
        return map;
    }
}
