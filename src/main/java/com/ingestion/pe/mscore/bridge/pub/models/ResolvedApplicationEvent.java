package com.ingestion.pe.mscore.bridge.pub.models;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResolvedApplicationEvent {
    private String eventId;
    private String resolutionNotes;
    private Boolean resolution;
    private Map<String, Object> resolutionProperties;
}
