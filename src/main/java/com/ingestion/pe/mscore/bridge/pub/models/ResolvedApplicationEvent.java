package com.ingestion.pe.mscore.bridge.pub.models;

import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolvedApplicationEvent {
    private UUID eventId;
    private String resolutionNotes;
    private boolean resolution;
    private Map<String, Object> resolutionProperties;
}
