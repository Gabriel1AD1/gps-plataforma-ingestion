package com.ingestion.pe.mscore.commons.models;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebsocketMessage {

    @Builder.Default
    private Boolean broadcast = false;

    @Builder.Default
    private MessageAgregateType messageAgregateType = MessageAgregateType.UNDEFINED;

    @Builder.Default
    private Collection<UUID> uuids = Set.of();

    @Builder.Default
    private MessageType messageType = MessageType.UNDEFINED;

    private String message;

    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    private Long companyId;

    /*
     * =======================
     * Builders semánticos
     * =======================
     */

    public static WebsocketMessageBuilder notificationBuilder() {
        return baseBuilder().messageType(MessageType.NOTIFICATION);
    }

    public static WebsocketMessageBuilder refreshBuilder() {
        return baseBuilder().messageType(MessageType.REFRESH);
    }

    private static WebsocketMessageBuilder baseBuilder() {
        return WebsocketMessage.builder()
                .broadcast(false)
                .uuids(Set.of())
                .properties(new HashMap<>())
                .messageAgregateType(MessageAgregateType.UNDEFINED);
    }

    /*
     * =======================
     * Enums
     * =======================
     */

    public enum MessageAgregateType {
        DEVICE_UPDATE,
        STATUS_DEVICE,
        CONNECTED_USER,
        FLEET_SUMMARY,
        SUMMARY_DEVICE,
        EVENTS_CREATED,
        VEHICLE_UPDATE, UNDEFINED
    }

    public enum MessageType {
        NOTIFICATION,
        REFRESH,
        UNDEFINED
    }

    public String toJson() {
        return JsonUtils.toJson(this);
    }
}
