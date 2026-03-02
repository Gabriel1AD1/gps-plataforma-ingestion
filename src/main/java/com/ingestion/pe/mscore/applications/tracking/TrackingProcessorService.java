package com.ingestion.pe.mscore.applications.tracking;

import com.ingestion.pe.mscore.applications.tracking.enums.GeofenceEventType;
import com.ingestion.pe.mscore.applications.tracking.enums.GeofenceStatus;
import com.ingestion.pe.mscore.bridge.pub.models.GeofenceEventDto;
import com.ingestion.pe.mscore.bridge.pub.service.KafkaBusinessEventPublisher;
import com.ingestion.pe.mscore.commons.models.GeofenceResponse;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.config.cache.store.GeofenceStatusCacheStore;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingProcessorService {

    private final DistanceCalculator distanceCalculator;
    private final GeofenceEvaluator geofenceEvaluator;
    private final GeofenceStatusCacheStore geofenceStatusCacheStore;
    private final KafkaBusinessEventPublisher kafkaBusinessEventPublisher;
    private final CacheDao<GeofenceResponse> geofenceCacheDao;
    private final RedisTemplate<String, Object> redisTemplate;

    public void processPositionForTracking(Position position, Long companyId) {
        if (position == null || companyId == null || companyId == 0L) {
            log.warn("No se puede procesar la posición: posición inválida o companyId faltante");
            return;
        }

        try {
            
            distanceCalculator.updateDistance(position, companyId);

            
            String companyGeofencesKey = "geofence:company:" + companyId;
            Object idsObj = redisTemplate.opsForValue().get(companyGeofencesKey);

            if (idsObj == null) {
                log.debug("No se encontraron geocercas activas para companyId={}", companyId);
                return;
            }

            List<Long> geofenceIds;
            if (idsObj instanceof List<?>) {
                geofenceIds = ((List<?>) idsObj).stream()
                        .filter(Objects::nonNull)
                        .map(obj -> Long.parseLong(obj.toString()))
                        .collect(Collectors.toList());
            } else {
                log.warn("Tipo inesperado para geocercas de la empresa en Redis: {}", idsObj.getClass());
                return;
            }

            if (geofenceIds.isEmpty()) {
                return;
            }

            log.debug("Revisando {} geocercas para IMEI={}", geofenceIds.size(), position.getImei());

            for (Long geofenceId : geofenceIds) {
                String geofenceKey = "geofence:" + companyId + ":" + geofenceId;
                Optional<GeofenceResponse> geofenceOpt = geofenceCacheDao.get(geofenceKey, GeofenceResponse.class);

                geofenceOpt.ifPresent(geofence -> detectGeofenceEvent(position, geofence, companyId));
            }

        } catch (Exception e) {
            log.error("Error al calcular métricas de seguimiento para la posición: {}", e.getMessage(), e);
        }
    }

    private void detectGeofenceEvent(Position position, GeofenceResponse geofence, Long companyId) {
        boolean currentlyInside = geofenceEvaluator.isInside(geofence, position.getLatitude(), position.getLongitude());
        GeofenceStatus currentStatus = currentlyInside ? GeofenceStatus.INSIDE : GeofenceStatus.OUTSIDE;

        Optional<GeofenceStatus> previousStatusOpt = geofenceStatusCacheStore.get(companyId, position.getImei(),
                geofence.getId());

        
        geofenceStatusCacheStore.save(companyId, position.getImei(), geofence.getId(), currentStatus);

        if (previousStatusOpt.isEmpty() || previousStatusOpt.get() == GeofenceStatus.UNKNOWN) {
            log.debug("Primera detección: IMEI={}, geofence={}, status={}", position.getImei(), geofence.getName(),
                    currentStatus);
            return;
        }

        GeofenceStatus previousStatus = previousStatusOpt.get();
        GeofenceEventType eventType = null;

        if (previousStatus == GeofenceStatus.OUTSIDE && currentStatus == GeofenceStatus.INSIDE) {
            eventType = GeofenceEventType.ENTRY;
        } else if (previousStatus == GeofenceStatus.INSIDE && currentStatus == GeofenceStatus.OUTSIDE) {
            eventType = GeofenceEventType.EXIT;
        }

        if (eventType != null) {
            publishEvent(position, geofence, companyId, eventType);
        }
    }

    private void publishEvent(Position position, GeofenceResponse geofence, Long companyId,
            GeofenceEventType eventType) {
        log.info("{} detectada: IMEI={}, geofence={}", eventType, position.getImei(), geofence.getName());

        GeofenceEventDto eventDto = GeofenceEventDto.builder()
                .imei(position.getImei())
                .companyId(companyId)
                .geofenceId(geofence.getId())
                .geofenceName(geofence.getName())
                .eventType(eventType.name())
                .latitude(position.getLatitude())
                .longitude(position.getLongitude())
                .deviceTime(position.getDeviceTime() != null ? position.getDeviceTime().toInstant() : Instant.now())
                .build();

        kafkaBusinessEventPublisher.publishGeofenceEvent(eventDto);
    }
}
