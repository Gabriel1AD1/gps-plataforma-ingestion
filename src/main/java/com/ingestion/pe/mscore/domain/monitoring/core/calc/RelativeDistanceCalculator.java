package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import com.ingestion.pe.mscore.domain.monitoring.core.model.ControlPointModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.DateroResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TimeMatrixModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TimeSpanModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelativeDistanceCalculator {

    private static final double DEFAULT_AVG_SPEED_KM_PER_MIN = 0.5; // ~30 km/h fallback

    private final TimeContextResolver timeContextResolver;

    public List<DateroResult> calculate(Long routeId,
            List<TripState> tripStates,
            RouteConfigResponse routeConfig) {

        List<DateroResult> results = new ArrayList<>();

        if (tripStates == null || tripStates.isEmpty()) {
            return results;
        }

        List<ControlPointModel> controlPoints = routeConfig != null ? routeConfig.getControlPoints() : null;
        Map<String, Double> matrixMap = buildMatrixMap(routeConfig);

        List<TripState> sorted = tripStates.stream()
                .filter(s -> s.getAccumulatedDistanceKm() != null)
                .sorted(Comparator.comparingDouble(TripState::getAccumulatedDistanceKm).reversed())
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            TripState current = sorted.get(i);

            String cpName = resolveControlPointName(current.getCurrentPointIndex(), controlPoints);

            DateroResult.DateroResultBuilder builder = DateroResult.builder()
                    .tripId(current.getTripId())
                    .vehicleId(current.getVehicleId())
                    .rank(i + 1)
                    .currentPointIndex(current.getCurrentPointIndex())
                    .currentControlPointName(cpName);

            if (i > 0) {
                TripState ahead = sorted.get(i - 1);
                double deltaMin = estimateDeltaMinutes(current, ahead, controlPoints, matrixMap);
                double deltaKm = ahead.getAccumulatedDistanceKm() - current.getAccumulatedDistanceKm();
                builder.aheadVehicleId(ahead.getVehicleId())
                        .aheadDeltaKm(Math.round(deltaKm * 100.0) / 100.0)
                        .aheadDeltaMinutes(Math.round(deltaMin * 10.0) / 10.0);
            }

            if (i < sorted.size() - 1) {
                TripState behind = sorted.get(i + 1);
                double deltaMin = estimateDeltaMinutes(behind, current, controlPoints, matrixMap);
                double deltaKm = current.getAccumulatedDistanceKm() - behind.getAccumulatedDistanceKm();
                builder.behindVehicleId(behind.getVehicleId())
                        .behindDeltaKm(Math.round(deltaKm * 100.0) / 100.0)
                        .behindDeltaMinutes(Math.round(deltaMin * 10.0) / 10.0);
            }

            results.add(builder.build());
        }

        return results;
    }

    private double estimateDeltaMinutes(TripState behind, TripState ahead,
            List<ControlPointModel> controlPoints,
            Map<String, Double> matrixMap) {

        if (controlPoints == null || controlPoints.isEmpty() || matrixMap.isEmpty()) {
            double deltaKm = ahead.getAccumulatedDistanceKm() - behind.getAccumulatedDistanceKm();
            return deltaKm <= 0 ? 0.0 : deltaKm / DEFAULT_AVG_SPEED_KM_PER_MIN;
        }

        int fromIdx = behind.getCurrentPointIndex();
        int toIdx = ahead.getCurrentPointIndex();

        if (fromIdx >= toIdx) {
            double deltaKm = ahead.getAccumulatedDistanceKm() - behind.getAccumulatedDistanceKm();
            return deltaKm <= 0 ? 0.0 : deltaKm / DEFAULT_AVG_SPEED_KM_PER_MIN;
        }

        double totalMinutes = 0.0;

        for (int i = fromIdx; i < toIdx && i + 1 < controlPoints.size(); i++) {
            Long fromId = controlPoints.get(i).getId();
            Long toId = controlPoints.get(i + 1).getId();
            String key = fromId + ":" + toId;
            totalMinutes += matrixMap.getOrDefault(key, 0.0);
        }

        if (totalMinutes <= 0) {
            double deltaKm = ahead.getAccumulatedDistanceKm() - behind.getAccumulatedDistanceKm();
            return deltaKm <= 0 ? 0.0 : deltaKm / DEFAULT_AVG_SPEED_KM_PER_MIN;
        }

        // Interpolación parcial para el segmento donde está el bus de atrás
        if (fromIdx + 1 < controlPoints.size()) {
            ControlPointModel cpFrom = controlPoints.get(fromIdx);
            ControlPointModel cpTo = controlPoints.get(fromIdx + 1);

            double segDistKm = getSegmentDistance(cpFrom, cpTo);
            if (segDistKm > 0) {
                double busAdvanceKm = behind.getAccumulatedDistanceKm()
                        - (cpFrom.getDistanceFromStartKm() != null ? cpFrom.getDistanceFromStartKm() : 0.0);
                double fraction = Math.max(0, Math.min(1, busAdvanceKm / segDistKm));

                String firstKey = cpFrom.getId() + ":" + cpTo.getId();
                double firstSegMinutes = matrixMap.getOrDefault(firstKey, 0.0);
                totalMinutes -= firstSegMinutes * fraction;
            }
        }

        return Math.max(0, totalMinutes);
    }

    private double getSegmentDistance(ControlPointModel from, ControlPointModel to) {
        double fromDist = from.getDistanceFromStartKm() != null ? from.getDistanceFromStartKm() : 0.0;
        double toDist = to.getDistanceFromStartKm() != null ? to.getDistanceFromStartKm() : 0.0;
        double diff = toDist - fromDist;
        if (diff > 0) {
            return diff;
        }
        return HaversineCalculator.distanceKm(
                from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude());
    }

    private Map<String, Double> buildMatrixMap(RouteConfigResponse config) {
        Map<String, Double> map = new HashMap<>();
        if (config == null || config.getTimeMatrix() == null || config.getTimeSpans() == null) {
            return map;
        }

        Optional<TimeSpanModel> spanOpt = timeContextResolver
                .resolveCurrentTimeSpan(config.getTimeSpans(), Instant.now());
        if (spanOpt.isEmpty()) {
            return map;
        }

        Long activeSpanId = spanOpt.get().getId();

        for (TimeMatrixModel tm : config.getTimeMatrix()) {
            if (activeSpanId.equals(tm.getTimeSpanId())
                    && tm.getFromControlPointId() != null
                    && tm.getToControlPointId() != null
                    && tm.getExpectedTravelMinutes() != null) {
                map.put(tm.getFromControlPointId() + ":" + tm.getToControlPointId(),
                        tm.getExpectedTravelMinutes());
            }
        }

        return map;
    }

    private String resolveControlPointName(int index, List<ControlPointModel> controlPoints) {
        if (controlPoints == null || index < 0 || index >= controlPoints.size()) {
            return null;
        }
        return controlPoints.get(index).getName();
    }
}
