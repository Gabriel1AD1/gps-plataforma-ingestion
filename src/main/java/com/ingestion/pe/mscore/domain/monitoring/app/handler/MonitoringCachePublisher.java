package com.ingestion.pe.mscore.domain.monitoring.app.handler;

import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.domain.monitoring.core.calc.LinearViewCalculator;
import com.ingestion.pe.mscore.domain.monitoring.core.calc.RelativeDistanceCalculator;
import com.ingestion.pe.mscore.domain.monitoring.core.model.DateroResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.LinearViewResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringCachePublisher {

    private final TripStateManager tripStateManager;
    private final RouteConfigClient routeConfigClient;
    private final LinearViewCalculator linearViewCalc;
    private final RelativeDistanceCalculator dateroCalc;
    private final CacheDao<Object> cacheDao;
    private final KafkaPublisherService kafkaPublisherService;

    private static final String LINEAR_VIEW_KEY_PREFIX = "monitoring:linearview:route:";
    private static final String DATERO_KEY_PREFIX = "monitoring:datero:route:";
    private static final long TTL_SECONDS = 86400; // 24h cache (event-driven refreshes)

    private static final long ROUTE_THROTTLE_MS = 500;
    private final ConcurrentHashMap<Long, Long> lastRouteRefreshTime = new ConcurrentHashMap<>();

    public void processRouteThrottled(Long routeId) {
        if (routeId == null) return;
        long now = System.currentTimeMillis();
        long last = lastRouteRefreshTime.getOrDefault(routeId, 0L);
        if ((now - last) < ROUTE_THROTTLE_MS) {
            log.debug("MonitoringCachePublisher: ruta {} throttled (<{}ms), skip", routeId, ROUTE_THROTTLE_MS);
            return;
        }
        lastRouteRefreshTime.put(routeId, now);
        processRoute(routeId);
    }


    public void processRoute(Long routeId) {
        try {
            List<TripState> states = tripStateManager.getStatesByRoute(routeId);
            if (states.isEmpty()) {

                log.info("MonitoringCachePublisher: ruta {} sin estados activos, limpiando Redis y notificando WS", routeId);
                clearRouteCache(routeId, null);
                return;
            }

            RouteConfigResponse config = routeConfigClient.getRouteConfig(routeId).orElse(null);
            if (config == null) {
                return;
            }

            List<LinearViewResult> linearView = linearViewCalc.calculate(routeId, states, config);

            List<DateroResult> datero = dateroCalc.calculate(routeId, states, config);
            cacheDao.save(DATERO_KEY_PREFIX + routeId, datero, TTL_SECONDS);

            for (LinearViewResult lv : linearView) {
                datero.stream().filter(d -> d.getVehicleId().equals(lv.getVehicleId())).findFirst().ifPresent(d -> {
                    lv.setAheadVehicleId(d.getAheadVehicleId());
                    lv.setAheadDeltaKm(d.getAheadDeltaKm());
                    lv.setAheadDeltaMinutes(d.getAheadDeltaMinutes());
                    lv.setBehindVehicleId(d.getBehindVehicleId());
                    lv.setBehindDeltaKm(d.getBehindDeltaKm());
                    lv.setBehindDeltaMinutes(d.getBehindDeltaMinutes());
                });
            }

            cacheDao.save(LINEAR_VIEW_KEY_PREFIX + routeId, linearView, TTL_SECONDS);

            Long companyId = states.get(0).getCompanyId();
            if (companyId != null) {
                publishRefresh(companyId, routeId);
            }

            log.debug("MonitoringCachePublisher: Actualizada ruta {} ({} vehículos)", routeId, states.size());

        } catch (Exception e) {
            log.error("Error procesando ruta {} en MonitoringCachePublisher: {}", routeId, e.getMessage());
        }
    }

    public void processRoute(Long routeId, Long companyId) {
        try {
            List<TripState> states = tripStateManager.getStatesByRoute(routeId);
            if (states.isEmpty()) {
                log.info("MonitoringCachePublisher: ruta {} quedó vacía, limpiando Redis y notificando WS (companyId={})", routeId, companyId);
                clearRouteCache(routeId, companyId);
            } else {
                processRoute(routeId);
            }
        } catch (Exception e) {
            log.error("Error en processRoute(routeId={}, companyId={}) en MonitoringCachePublisher: {}", routeId, companyId, e.getMessage());
        }
    }

    private void clearRouteCache(Long routeId, Long companyId) {
        try {
            cacheDao.delete(LINEAR_VIEW_KEY_PREFIX + routeId);
            cacheDao.delete(DATERO_KEY_PREFIX + routeId);
            log.info("MonitoringCachePublisher: Caché de ruta {} limpiada (lista vacía de vehículos)", routeId);

            if (companyId != null) {
                publishRefresh(companyId, routeId);
            }
        } catch (Exception e) {
            log.error("Error limpiando caché de ruta {} en MonitoringCachePublisher: {}", routeId, e.getMessage());
        }
    }

    private void publishRefresh(Long companyId, Long routeId) {
        WebsocketMessage msg = WebsocketMessage.refreshBuilder()
                .messageAgregateType(WebsocketMessage.MessageAgregateType.MONITORING_REFRESH)
                .companyId(companyId)
                .properties(Map.of("routeId", routeId))
                .build();
        kafkaPublisherService.publishWebsocketMessage(msg);
    }
}
