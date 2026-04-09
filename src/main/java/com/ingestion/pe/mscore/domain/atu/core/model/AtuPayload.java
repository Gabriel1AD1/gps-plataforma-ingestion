package com.ingestion.pe.mscore.domain.atu.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AtuPayload {

    @JsonProperty("imei")
    private String imei;

    @JsonProperty("latitude")
    private double latitude;

    @JsonProperty("longitude")
    private double longitude;

    @JsonProperty("route_id")
    private String routeId;

    @JsonProperty("ts")
    private long ts;

    @JsonProperty("license_plate")
    private String licensePlate;

    @JsonProperty("speed")
    private double speed;

    @JsonProperty("direction_id")
    private int directionId;

    @JsonProperty("driver_id")
    private String driverId;

    @JsonProperty("tsinitialtrip")
    private long tsInitialTrip;

    @JsonProperty("identifier")
    private String identifier;
}
