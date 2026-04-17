package com.ingestion.pe.mscore.domain.monitoring.app.handler;

import com.ingestion.pe.mscore.domain.monitoring.core.model.TripActiveResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final MonitoringCachePublisher monitoringCachePublisher;

    private final Set<Long> knownTripIds = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelay = 1_000, initialDelay = 5_000)
    public void pollAndInitialize() {
        try {
            List<Long> routeIds = routeConfigClient.getKnownRouteIds();

            Set<Long> allActiveTripIds = new HashSet<>();
            Set<Long> routesToUpdate = new HashSet<>();
            Map<Long, Long> routeCompanyIds = new HashMap<>();

            if (!routeIds.isEmpty()) {
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

                    routesToUpdate.add(tripOpt.get().getRouteId());

                    log.info("TripState auto-inicializado para tripId={}", tripId);
                }
            }

            cleanupStaleStates(allActiveTripIds, routesToUpdate, routeCompanyIds);

            for (Long routeId : routesToUpdate) {
                Long companyId = routeCompanyIds.get(routeId);
                monitoringCachePublisher.processRoute(routeId, companyId);
            }
        } catch (Exception e) {
            log.error("Error en TripStateInitializer poll", e);
        }
    }

    private void cleanupStaleStates(Set<Long> activeTripIds, Set<Long> routesToUpdate, Map<Long, Long> routeCompanyIds) {
        knownTripIds.removeIf(tripId -> {
            if (!activeTripIds.contains(tripId)) {
                Optional<com.ingestion.pe.mscore.domain.monitoring.core.model.TripState> stateOpt = tripStateManager.getState(tripId);
                if (stateOpt.isPresent()) {
                    Long routeId = stateOpt.get().getRouteId();
                    Long companyId = stateOpt.get().getCompanyId();
                    tripStateManager.removeState(tripId);

                    routesToUpdate.add(routeId);
                    if (companyId != null) {
                        routeCompanyIds.put(routeId, companyId);
                    }
                } else {
                    tripStateManager.removeState(tripId);
                }
                log.info("TripState removido para tripId={} (ya no activo)", tripId);
                return true;
            }
            return false;
        });
    }
}
