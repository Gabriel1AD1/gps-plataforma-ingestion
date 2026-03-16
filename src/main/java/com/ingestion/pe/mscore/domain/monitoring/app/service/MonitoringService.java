package com.ingestion.pe.mscore.domain.monitoring.app.service;

import com.ingestion.pe.mscore.domain.monitoring.core.calc.LinearViewCalculator;
import com.ingestion.pe.mscore.domain.monitoring.core.calc.RelativeDistanceCalculator;
import com.ingestion.pe.mscore.domain.monitoring.core.model.DateroResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.LinearViewResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final TripStateManager tripStateManager;
    private final LinearViewCalculator linearViewCalc;
    private final RelativeDistanceCalculator dateroCalc;
    private final RouteConfigClient routeConfigClient;

    public List<LinearViewResult> getLinearView(Long routeId) {
        List<TripState> states = tripStateManager.getStatesByRoute(routeId);
        RouteConfigResponse config = routeConfigClient.getRouteConfig(routeId)
                .orElse(null);

        if (config == null) {
            return List.of();
        }

        return linearViewCalc.calculate(routeId, states, config);
    }

    public List<DateroResult> getDatero(Long routeId) {
        List<TripState> states = tripStateManager.getStatesByRoute(routeId);
        RouteConfigResponse config = routeConfigClient.getRouteConfig(routeId)
                .orElse(null);

        if (config == null) {
            return List.of();
        }

        return dateroCalc.calculate(routeId, states, config);
    }

    public Optional<TripState> getTripState(Long tripId) {
        return tripStateManager.getState(tripId);
    }

    public int getActiveTripsCount() {
        return tripStateManager.getActiveCount();
    }
}
