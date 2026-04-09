package com.ingestion.pe.mscore.domain.monitoring.core.model;

import java.io.Serializable;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeSpanModel implements Serializable {
    private Long id;
    private String name;
    private Integer dayOfWeekMask;
    private LocalTime startTime;
    private LocalTime endTime;
}
