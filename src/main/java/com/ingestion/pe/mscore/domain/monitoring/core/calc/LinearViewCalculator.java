package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import com.ingestion.pe.mscore.domain.monitoring.core.model.ControlPointModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.LinearViewResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LinearViewCalculator {

    /**
     * Calcula el porcentaje de progreso de cada bus activo en una ruta.
     *
     * Algoritmo:
     * 1. distanciaBase = controlPoints[currentPointIndex].distanceFromStartKm
     * 2. distanciaSegmento = Haversine(lastControlPoint, posiciónBus)
     * 3. distanciaMaxSegmento = distancia entre lastCP y nextCP
     * 4. distanciaTotal = distanciaBase + min(distanciaSegmento,
     * distanciaMaxSegmento)
     * 5. progress = (distanciaTotal / route.totalDistanceKm) * 100
     * 6. Clampar a [0, 100]
     */
    public List<LinearViewResult> calculate(Long routeId,
            List<TripState> tripStates,
            RouteConfigResponse routeConfig) {

        List<LinearViewResult> results = new ArrayList<>();

        if (routeConfig == null || routeConfig.getTotalDistanceKm() == null
                || routeConfig.getTotalDistanceKm() <= 0) {
            return results;
        }

        List<ControlPointModel> controlPoints = routeConfig.getControlPoints();
        if (controlPoints == null || controlPoints.isEmpty()) {
            return results;
        }

        double totalDistance = routeConfig.getTotalDistanceKm();

        for (TripState state : tripStates) {
            try {
                double progress = calculateProgress(state, controlPoints, totalDistance);

                LinearViewResult result = LinearViewResult.builder()
                        .tripId(state.getTripId())
                        .vehicleId(state.getVehicleId())
                        .driverId(state.getDriverId())
                        .progressPercent(progress)
                        .status(state.getStatus())
                        .direction(state.getDirection())
                        .lastUpdateTime(state.getLastUpdateTime())
                        .build();

                results.add(result);
            } catch (Exception e) {
                log.error("Error calculando progreso para tripId={}: {}", state.getTripId(), e.getMessage());
            }
        }

        return results;
    }

    private double calculateProgress(TripState state, List<ControlPointModel> controlPoints,
            double totalRouteDistance) {

        int currentIndex = state.getCurrentPointIndex();
        String direction = state.getDirection();

        if (currentIndex < 0 || currentIndex >= controlPoints.size()) {
            return 0.0;
        }

        ControlPointModel currentCP = controlPoints.get(currentIndex);

        double baseDistance = currentCP.getDistanceFromStartKm() != null
                ? currentCP.getDistanceFromStartKm()
                : 0.0;

        double segmentDistance = 0.0;
        if (state.getLastLatitude() != null && state.getLastLongitude() != null) {
            segmentDistance = HaversineCalculator.distanceKm(
                    currentCP.getLatitude(), currentCP.getLongitude(),
                    state.getLastLatitude(), state.getLastLongitude());

            if (currentIndex + 1 < controlPoints.size()) {
                ControlPointModel nextCP = controlPoints.get(currentIndex + 1);
                double maxSegment = HaversineCalculator.distanceKm(
                        currentCP.getLatitude(), currentCP.getLongitude(),
                        nextCP.getLatitude(), nextCP.getLongitude());
                segmentDistance = Math.min(segmentDistance, maxSegment);
            }
        }

        double totalAcumulado = baseDistance + segmentDistance;
        state.setAccumulatedDistanceKm(totalAcumulado);

        double minDirDist = 0.0;
        double maxDirDist = totalRouteDistance;

        if (direction != null && !"LOOP".equalsIgnoreCase(direction)) {
            double minFound = Double.MAX_VALUE;
            double maxFound = -1.0;
            boolean found = false;

            for (ControlPointModel cp : controlPoints) {
                if (direction.equalsIgnoreCase(cp.getDirection())) {
                    double d = cp.getDistanceFromStartKm() != null ? cp.getDistanceFromStartKm() : 0.0;
                    if (d < minFound) minFound = d;
                    if (d > maxFound) maxFound = d;
                    found = true;
                }
            }

            if (found) {
                minDirDist = minFound;
                maxDirDist = maxFound;
            }
        }

        double denominator = maxDirDist - minDirDist;
        if (denominator <= 0) {
            double progress = (totalAcumulado / totalRouteDistance) * 100.0;
            return Math.max(0.0, Math.min(100.0, progress));
        }

        double progress = ((totalAcumulado - minDirDist) / denominator) * 100.0;
        return Math.max(0.0, Math.min(100.0, progress));
    }
}
