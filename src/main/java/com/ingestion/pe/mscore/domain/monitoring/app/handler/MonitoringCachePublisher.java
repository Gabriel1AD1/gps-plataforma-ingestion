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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import java.util.List;
import java.util.Map;

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
    private static final long TTL_SECONDS = 60; // 1 min cache for these calculated views

    @Scheduled(fixedDelay = 15000, initialDelay = 10000)
    public void publishMonitoringData() {
        try {
            List<Long> routeIds = routeConfigClient.getKnownRouteIds();
            if (routeIds.isEmpty()) {
                return;
            }

            for (Long routeId : routeIds) {
                processRoute(routeId);
            }
        } catch (Exception e) {
            log.error("Error en MonitoringCachePublisher: {}", e.getMessage());
        }
    }

    private void processRoute(Long routeId) {
        try {
            List<TripState> states = tripStateManager.getStatesByRoute(routeId);
            if (states.isEmpty()) {
                // Opcional: limpiar cache si no hay viajes activos
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
                WebsocketMessage msg = WebsocketMessage.refreshBuilder()
                        .messageAgregateType(WebsocketMessage.MessageAgregateType.MONITORING_REFRESH)
                        .companyId(companyId)
                        .properties(Map.of("routeId", routeId))
                        .build();
                kafkaPublisherService.publishWebsocketMessage(msg);
            }

            log.debug("MonitoringCachePublisher: Actualizada ruta {} ({} vehículos)", routeId, states.size());

        } catch (Exception e) {
            log.error("Error procesando ruta {} en MonitoringCachePublisher: {}", routeId, e.getMessage());
        }
    }
}
