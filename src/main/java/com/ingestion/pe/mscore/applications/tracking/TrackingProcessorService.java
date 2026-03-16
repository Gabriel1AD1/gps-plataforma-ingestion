package com.ingestion.pe.mscore.applications.tracking;

import com.ingestion.pe.mscore.applications.tracking.enums.GeofenceEventType;
import com.ingestion.pe.mscore.applications.tracking.enums.GeofenceStatus;
import com.ingestion.pe.mscore.bridge.pub.models.ApplicationEventCreate;
import com.ingestion.pe.mscore.bridge.pub.models.GeofenceEventDto;
import com.ingestion.pe.mscore.bridge.pub.service.KafkaBusinessEventPublisher;
import com.ingestion.pe.mscore.commons.models.GeofenceResponse;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.config.cache.store.GeofenceStatusCacheStore;
import com.ingestion.pe.mscore.domain.devices.app.factory.GeofenceApplicationEventFactory;
import com.ingestion.pe.mscore.domain.devices.app.resolver.EventResolver;
import com.ingestion.pe.mscore.domain.devices.core.entity.EventEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.geofences.core.entity.GeofenceReadEntity;
import com.ingestion.pe.mscore.domain.geofences.core.repo.GeofenceReadEntityRepository;
import com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleGeofenceEntity;
import com.ingestion.pe.mscore.domain.vehicles.core.repo.VehicleGeofenceEntityRepository;
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
    private final VehicleGeofenceEntityRepository vehicleGeofenceEntityRepository;
    private final EventEntityRepository eventEntityRepository;
    private final EventResolver eventResolver;
    private final GeofenceReadEntityRepository geofenceReadEntityRepository;

    private static final long GEOFENCE_CACHE_TTL_SECONDS = 600;

    public void processPositionForTracking(Position position, Long companyId) {
        if (position == null || companyId == null || companyId == 0L) {
            log.warn("No se puede procesar la posición: posición inválida o companyId faltante");
            return;
        }

        try {
            distanceCalculator.updateDistance(position, companyId);

            String companyGeofencesKey = "geofence:company:" + companyId;
            Object idsObj = redisTemplate.opsForValue().get(companyGeofencesKey);

            List<Long> geofenceIds;
            if (idsObj instanceof List<?>) {
                geofenceIds = ((List<?>) idsObj).stream()
                        .filter(Objects::nonNull)
                        .map(obj -> Long.parseLong(obj.toString()))
                        .collect(Collectors.toList());
            } else if (idsObj == null) {
                // Cache miss para la lista de empresa — fallback a PostgreSQL
                log.debug("Cache miss en geofence:company:{} — consultando PostgreSQL", companyId);
                geofenceIds = geofenceReadEntityRepository.findAllByCompanyId(companyId)
                        .stream()
                        .map(GeofenceReadEntity::getId)
                        .collect(Collectors.toList());
                if (geofenceIds.isEmpty()) {
                    log.debug("No se encontraron geocercas activas para companyId={}", companyId);
                    return;
                }
            } else {
                log.warn("Tipo inesperado para geocercas de la empresa en Redis: {}", idsObj.getClass());
                return;
            }

            if (geofenceIds.isEmpty()) {
                return;
            }

            log.debug("Revisando {} geocercas para IMEI={}", geofenceIds.size(), position.getImei());

            for (Long geofenceId : geofenceIds) {
                GeofenceResponse geofence = getGeofenceWithFallback(geofenceId, companyId);
                if (geofence == null) {
                    log.warn("Geocerca id={} no encontrada en Redis ni PostgreSQL, se omite", geofenceId);
                    continue;
                }
                detectGeofenceEvent(position, geofence, companyId);
            }

        } catch (Exception e) {
            log.error("Error al calcular métricas de seguimiento para la posición: {}", e.getMessage(), e);
        }
    }

    private GeofenceResponse getGeofenceWithFallback(Long geofenceId, Long companyId) {
        String geofenceKey = "geofence:" + companyId + ":" + geofenceId;
        Optional<GeofenceResponse> cached = geofenceCacheDao.get(geofenceKey, GeofenceResponse.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        log.debug("Cache miss geofence id={} companyId={} — consultando PostgreSQL", geofenceId, companyId);
        Optional<GeofenceReadEntity> entityOpt = geofenceReadEntityRepository.findById(geofenceId);
        if (entityOpt.isEmpty()) {
            return null;
        }

        GeofenceReadEntity entity = entityOpt.get();
        GeofenceResponse response = toGeofenceResponse(entity);

        vehicleGeofenceEntityRepository.findFirstByGeofenceId(geofenceId)
                .ifPresent(vg -> response.setVehicleId(vg.getVehicleId()));

        geofenceCacheDao.save(geofenceKey, response, GEOFENCE_CACHE_TTL_SECONDS);
        log.debug("Geocerca id={} cargada de PostgreSQL y guardada en Redis", geofenceId);

        return response;
    }

    private GeofenceResponse toGeofenceResponse(GeofenceReadEntity entity) {
        GeofenceResponse r = new GeofenceResponse();
        r.setId(entity.getId());
        r.setCompanyId(entity.getCompanyId());
        r.setName(entity.getName());
        r.setDescription(entity.getDescription());
        r.setLatitudeCenter(entity.getLatitudeCenter());
        r.setLongitudeCenter(entity.getLongitudeCenter());
        r.setRadiusInMeters(entity.getRadiusInMeters());
        r.setType(entity.getType());
        r.setPoints(null);
        return r;
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

        // kafkaBusinessEventPublisher.publishGeofenceEvent(eventDto);

        VehicleGeofenceEntity geofenceConfig;
        if (geofence.getVehicleId() != null) {
            geofenceConfig = vehicleGeofenceEntityRepository
                    .findByVehicleIdAndGeofenceId(geofence.getVehicleId(), geofence.getId())
                    .orElse(null);
        } else {
            geofenceConfig = vehicleGeofenceEntityRepository
                    .findFirstByGeofenceId(geofence.getId())
                    .orElse(null);
        }
        if (geofenceConfig == null) {
            log.debug("Sin configuracion de notificación activa para geofenceId={}", geofence.getId());
        }

        ApplicationEventCreate appEvent = GeofenceApplicationEventFactory.forGeofenceEvent(eventDto, geofenceConfig);
        try {
            EventEntity eventEntity = EventEntity.map(appEvent);
            eventEntity = eventEntityRepository.save(eventEntity);
            eventResolver.resolveEvent(eventEntity);
        } catch (Exception e) {
            log.error("Error persistiendo EventEntity de geocerca IMEI={} geofence={}: {}",
                    position.getImei(), geofence.getId(), e.getMessage(), e);
        }
    }
}
