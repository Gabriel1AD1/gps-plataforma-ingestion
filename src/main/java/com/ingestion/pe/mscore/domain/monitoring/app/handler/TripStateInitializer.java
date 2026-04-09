package com.ingestion.pe.mscore.domain.monitoring.app.handler;

import com.ingestion.pe.mscore.domain.monitoring.core.model.TripActiveResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripStateInitializer {

    private final TripStateManager tripStateManager;
    private final PositionMonitoringHook positionMonitoringHook;
    private final RouteConfigClient routeConfigClient;

    private final Set<Long> knownTripIds = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
    public void pollAndInitialize() {
        try {
            List<Long> routeIds = routeConfigClient.getKnownRouteIds();
            if (routeIds.isEmpty()) {
                cleanupStaleStates(Set.of());
                return;
            }

            Set<Long> allActiveTripIds = new HashSet<>();

            for (Long routeId : routeIds) {
                List<Long> tripIds = routeConfigClient.getActiveTripsForRoute(routeId);
                allActiveTripIds.addAll(tripIds);
            }

            for (Long tripId : allActiveTripIds) {
                if (knownTripIds.contains(tripId)) {
                    continue;
                }

                if (tripStateManager.getState(tripId).isPresent()) {
                    knownTripIds.add(tripId);
                    continue;
                }

                Optional<TripActiveResponse> tripOpt = routeConfigClient.getActiveTrip(tripId);
                if (tripOpt.isEmpty()) {
                    continue;
                }

                positionMonitoringHook.initializeTripState(tripOpt.get());
                knownTripIds.add(tripId);
                log.info("TripState auto-inicializado para tripId={}", tripId);
            }

            cleanupStaleStates(allActiveTripIds);
        } catch (Exception e) {
            log.error("Error en TripStateInitializer poll", e);
        }
    }

    private void cleanupStaleStates(Set<Long> activeTripIds) {
        knownTripIds.removeIf(tripId -> {
            if (!activeTripIds.contains(tripId)) {
                tripStateManager.removeState(tripId);
                log.info("TripState removido para tripId={} (ya no activo)", tripId);
                return true;
            }
            return false;
        });
    }
}
