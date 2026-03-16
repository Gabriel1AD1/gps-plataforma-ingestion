package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import static org.junit.jupiter.api.Assertions.*;

import com.ingestion.pe.mscore.domain.monitoring.core.model.ControlPointModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.LinearViewResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinearViewCalculatorTest {

    private LinearViewCalculator calculator;
    private List<ControlPointModel> controlPoints;
    private RouteConfigResponse config;

    @BeforeEach
    void setUp() {
        calculator = new LinearViewCalculator();

        controlPoints = List.of(
                ControlPointModel.builder().id(10L).name("Inicio")
                        .latitude(-12.0).longitude(-77.0)
                        .distanceFromStartKm(0.0).sequenceOrder(0).build(),
                ControlPointModel.builder().id(20L).name("Medio")
                        .latitude(-12.05).longitude(-77.05)
                        .distanceFromStartKm(5.0).sequenceOrder(1).build(),
                ControlPointModel.builder().id(30L).name("Final")
                        .latitude(-12.10).longitude(-77.10)
                        .distanceFromStartKm(10.0).sequenceOrder(2).build());

        config = RouteConfigResponse.builder()
                .controlPoints(controlPoints)
                .totalDistanceKm(10.0)
                .build();
    }

    @Test
    void calculate_nullConfig_returnsEmpty() {
        List<LinearViewResult> results = calculator.calculate(1L, List.of(), null);
        assertTrue(results.isEmpty());
    }

    @Test
    void calculate_zeraTotalDistance_returnsEmpty() {
        RouteConfigResponse zeroConfig = RouteConfigResponse.builder()
                .controlPoints(controlPoints).totalDistanceKm(0.0).build();
        List<LinearViewResult> results = calculator.calculate(1L, List.of(), zeroConfig);
        assertTrue(results.isEmpty());
    }

    @Test
    void calculate_busAtStart_zeroProgress() {
        TripState state = TripState.builder()
                .tripId(1L).vehicleId(100L)
                .currentPointIndex(0)
                .lastLatitude(-12.0).lastLongitude(-77.0)
                .status("ON_TIME").direction("OUTBOUND")
                .build();

        List<LinearViewResult> results = calculator.calculate(1L, List.of(state), config);
        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getProgressPercent(), 1.0);
    }

    @Test
    void calculate_busAtEnd_nearHundredPercent() {
        TripState state = TripState.builder()
                .tripId(1L).vehicleId(100L)
                .currentPointIndex(2)
                .lastLatitude(-12.10).lastLongitude(-77.10)
                .status("ON_TIME").direction("OUTBOUND")
                .build();

        List<LinearViewResult> results = calculator.calculate(1L, List.of(state), config);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getProgressPercent() >= 99.0);
    }

    @Test
    void calculate_busAtMiddle_fiftyPercent() {
        TripState state = TripState.builder()
                .tripId(1L).vehicleId(100L)
                .currentPointIndex(1)
                .lastLatitude(-12.05).lastLongitude(-77.05) // punto medio
                .status("DELAYED").direction("OUTBOUND")
                .build();

        List<LinearViewResult> results = calculator.calculate(1L, List.of(state), config);
        assertEquals(1, results.size());
        double progress = results.get(0).getProgressPercent();
        assertTrue(progress >= 45.0 && progress <= 55.0,
                "Progreso en punto medio debe ser ~50%, fue: " + progress);
    }

    @Test
    void calculate_multipleBuses_returnsAll() {
        TripState bus1 = TripState.builder()
                .tripId(1L).vehicleId(100L).currentPointIndex(0)
                .lastLatitude(-12.0).lastLongitude(-77.0)
                .status("ON_TIME").direction("OUTBOUND").build();
        TripState bus2 = TripState.builder()
                .tripId(2L).vehicleId(200L).currentPointIndex(2)
                .lastLatitude(-12.10).lastLongitude(-77.10)
                .status("ON_TIME").direction("OUTBOUND").build();

        List<LinearViewResult> results = calculator.calculate(1L, List.of(bus1, bus2), config);
        assertEquals(2, results.size());
    }

    @Test
    void calculate_preservesStatusAndDirection() {
        TripState state = TripState.builder()
                .tripId(1L).vehicleId(100L)
                .currentPointIndex(1)
                .lastLatitude(-12.05).lastLongitude(-77.05)
                .status("DELAYED").direction("RETURN")
                .lastUpdateTime(Instant.now())
                .build();

        List<LinearViewResult> results = calculator.calculate(1L, List.of(state), config);
        assertEquals("DELAYED", results.get(0).getStatus());
        assertEquals("RETURN", results.get(0).getDirection());
        assertNotNull(results.get(0).getLastUpdateTime());
    }

    @Test
    void calculate_progressClampedAt100() {
        TripState state = TripState.builder()
                .tripId(1L).vehicleId(100L)
                .currentPointIndex(2)
                .lastLatitude(-12.20).lastLongitude(-77.20) 
                .status("ON_TIME").direction("OUTBOUND")
                .build();

        List<LinearViewResult> results = calculator.calculate(1L, List.of(state), config);
        assertTrue(results.get(0).getProgressPercent() <= 100.0,
                "Progreso no debe exceder 100%");
    }
}
