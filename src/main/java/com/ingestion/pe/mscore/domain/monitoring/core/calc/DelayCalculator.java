package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import com.ingestion.pe.mscore.domain.monitoring.core.model.ControlPointModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TimeMatrixModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TimeSpanModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelayCalculator {

    private static final double DELAY_THRESHOLD_MINUTES = 5.0;
    private static final double EARLY_THRESHOLD_MINUTES = -3.0;

    private final TimeContextResolver timeContextResolver;

    public void evaluate(TripState state, RouteConfigResponse config) {
        if (state.getDispatchTime() == null) {
            return;
        }
        if (config == null || config.getTimeMatrix() == null || config.getTimeSpans() == null) {
            return;
        }

        Optional<TimeSpanModel> spanOpt = timeContextResolver
                .resolveCurrentTimeSpan(config.getTimeSpans(), Instant.now());
        if (spanOpt.isEmpty()) {
            return;
        }

        Long activeSpanId = spanOpt.get().getId();

        double expectedMinutes = sumExpectedMinutes(
                config.getControlPoints(), config.getTimeMatrix(),
                activeSpanId, state.getCurrentPointIndex());

        if (expectedMinutes <= 0) {
            return;
        }

        double realMinutes = Duration.between(state.getDispatchTime(), Instant.now()).toSeconds() / 60.0;
        double delayMinutes = realMinutes - expectedMinutes;

        state.setAccumulatedDelayMinutes(Math.round(delayMinutes * 10.0) / 10.0);

        if (delayMinutes > DELAY_THRESHOLD_MINUTES) {
            state.setStatus("DELAYED");
        } else if (delayMinutes < EARLY_THRESHOLD_MINUTES) {
            state.setStatus("EARLY");
        } else {
            state.setStatus("ON_TIME");
        }
    }

    private double sumExpectedMinutes(List<ControlPointModel> controlPoints,
            List<TimeMatrixModel> timeMatrix,
            Long timeSpanId, int upToIndex) {

        if (controlPoints == null || controlPoints.isEmpty() || upToIndex <= 0) {
            return 0.0;
        }

        double total = 0.0;

        for (int i = 0; i < upToIndex && i + 1 < controlPoints.size(); i++) {
            Long fromId = controlPoints.get(i).getId();
            Long toId = controlPoints.get(i + 1).getId();

            for (TimeMatrixModel tm : timeMatrix) {
                if (timeSpanId.equals(tm.getTimeSpanId())
                        && fromId.equals(tm.getFromControlPointId())
                        && toId.equals(tm.getToControlPointId())
                        && tm.getExpectedTravelMinutes() != null) {
                    total += tm.getExpectedTravelMinutes();
                    break;
                }
            }
        }

        return total;
    }
}
