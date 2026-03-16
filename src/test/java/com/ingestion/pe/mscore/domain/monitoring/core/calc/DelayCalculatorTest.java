package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.ingestion.pe.mscore.domain.monitoring.core.model.*;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelayCalculatorTest {

    @Mock
    private TimeContextResolver timeContextResolver;

    @InjectMocks
    private DelayCalculator delayCalculator;

    private RouteConfigResponse config;
    private TimeSpanModel activeSpan;
    private List<ControlPointModel> controlPoints;
    private List<TimeMatrixModel> timeMatrix;

    @BeforeEach
    void setUp() {
        activeSpan = TimeSpanModel.builder()
                .id(1L).name("Mañana")
                .dayOfWeekMask(127)
                .startTime(LocalTime.of(5, 0))
                .endTime(LocalTime.of(23, 0))
                .build();

        controlPoints = List.of(
                ControlPointModel.builder().id(10L).name("A").sequenceOrder(0)
                        .latitude(-12.0).longitude(-77.0).distanceFromStartKm(0.0).build(),
                ControlPointModel.builder().id(20L).name("B").sequenceOrder(1)
                        .latitude(-12.01).longitude(-77.01).distanceFromStartKm(5.0).build(),
                ControlPointModel.builder().id(30L).name("C").sequenceOrder(2)
                        .latitude(-12.02).longitude(-77.02).distanceFromStartKm(10.0).build());

        timeMatrix = List.of(
                TimeMatrixModel.builder()
                        .fromControlPointId(10L).toControlPointId(20L)
                        .timeSpanId(1L).expectedTravelMinutes(5.0).build(),
                TimeMatrixModel.builder()
                        .fromControlPointId(20L).toControlPointId(30L)
                        .timeSpanId(1L).expectedTravelMinutes(3.0).build());

        config = RouteConfigResponse.builder()
                .controlPoints(controlPoints)
                .timeMatrix(timeMatrix)
                .timeSpans(List.of(activeSpan))
                .totalDistanceKm(10.0)
                .build();
    }

    @Test
    void evaluate_noDispatchTime_doesNothing() {
        TripState state = TripState.builder()
                .tripId(1L).currentPointIndex(1).build();

        delayCalculator.evaluate(state, config);
        assertNull(state.getStatus());
    }

    @Test
    void evaluate_nullConfig_doesNothing() {
        TripState state = TripState.builder()
                .tripId(1L).dispatchTime(Instant.now()).build();

        delayCalculator.evaluate(state, null);
        assertNull(state.getStatus());
    }

    @Test
    void evaluate_noActiveTimeSpan_doesNothing() {
        when(timeContextResolver.resolveCurrentTimeSpan(anyList(), any(Instant.class)))
                .thenReturn(Optional.empty());

        TripState state = TripState.builder()
                .tripId(1L).dispatchTime(Instant.now())
                .currentPointIndex(1).build();

        delayCalculator.evaluate(state, config);
        assertNull(state.getStatus());
    }

    @Test
    void evaluate_onTime_setsOnTime() {
        when(timeContextResolver.resolveCurrentTimeSpan(anyList(), any(Instant.class)))
                .thenReturn(Optional.of(activeSpan));

        TripState state = TripState.builder()
                .tripId(1L)
                .dispatchTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                .currentPointIndex(1)
                .build();

        delayCalculator.evaluate(state, config);
        assertEquals("ON_TIME", state.getStatus());
    }

    @Test
    void evaluate_delayed_setsDelayed() {
        when(timeContextResolver.resolveCurrentTimeSpan(anyList(), any(Instant.class)))
                .thenReturn(Optional.of(activeSpan));

        TripState state = TripState.builder()
                .tripId(1L)
                .dispatchTime(Instant.now().minus(15, ChronoUnit.MINUTES))
                .currentPointIndex(1)
                .build();

        delayCalculator.evaluate(state, config);
        assertEquals("DELAYED", state.getStatus());
        assertTrue(state.getAccumulatedDelayMinutes() > 5.0);
    }

    @Test
    void evaluate_early_setsEarly() {
        when(timeContextResolver.resolveCurrentTimeSpan(anyList(), any(Instant.class)))
                .thenReturn(Optional.of(activeSpan));

        TripState state = TripState.builder()
                .tripId(1L)
                .dispatchTime(Instant.now().minus(2, ChronoUnit.MINUTES))
                .currentPointIndex(2)
                .build();

        delayCalculator.evaluate(state, config);
        assertEquals("EARLY", state.getStatus());
        assertTrue(state.getAccumulatedDelayMinutes() < -3.0);
    }

    @Test
    void evaluate_atIndexZero_returnsEarlyNoExpectedTime() {
        when(timeContextResolver.resolveCurrentTimeSpan(anyList(), any(Instant.class)))
                .thenReturn(Optional.of(activeSpan));

        TripState state = TripState.builder()
                .tripId(1L)
                .dispatchTime(Instant.now().minus(1, ChronoUnit.MINUTES))
                .currentPointIndex(0)
                .build();

        delayCalculator.evaluate(state, config);
        assertNull(state.getStatus());
    }
}
