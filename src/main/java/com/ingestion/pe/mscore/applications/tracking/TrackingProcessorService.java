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
import java.util.*;
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
    private final com.ingestion.pe.mscore.domain.devices.core.repo.UserDeviceEntityRepository userDeviceEntityRepository;

    private static final long GEOFENCE_CACHE_TTL_SECONDS = 600;

    public void processPositionsBatchForTracking(String imei, List<Position> positions, Long companyId) {
        if (positions == null || positions.isEmpty() || companyId == null || companyId == 0L) {
            return;
        }

        log.debug("Procesando lote de tracking para IMEI {}: {} posiciones", imei, positions.size());

        // Cache de exclusiones
        Set<UUID> excludedUsers = userDeviceEntityRepository.findExcludedUuidsByDeviceImei(imei);

        for (Position position : positions) {
            processSinglePositionInternal(position, companyId, excludedUsers);
        }
    }

    public void processPositionForTracking(Position position, Long companyId) {
        if (position == null || companyId == null || companyId == 0L) return;
        Set<UUID> excludedUsers = userDeviceEntityRepository.findExcludedUuidsByDeviceImei(position.getImei());
        processSinglePositionInternal(position, companyId, excludedUsers);
    }

    private void processSinglePositionInternal(Position position, Long companyId, Set<UUID> excludedUsers) {
        try {
            distanceCalculator.updateDistance(position, companyId);

            List<Long> geofenceIds = getCompanyGeofenceIds(companyId);
            if (geofenceIds.isEmpty()) return;

            for (Long geofenceId : geofenceIds) {
                GeofenceResponse geofence = getGeofenceWithFallback(geofenceId, companyId);
                if (geofence == null) continue;
                detectGeofenceEvent(position, geofence, companyId, excludedUsers);
            }
        } catch (Exception e) {
            log.error("Error en tracking para IMEI {}: {}", position.getImei(), e.getMessage());
        }
    }

    private List<Long> getCompanyGeofenceIds(Long companyId) {
        String companyGeofencesKey = "geofence:company:" + companyId;
        Object idsObj = redisTemplate.opsForValue().get(companyGeofencesKey);

        if (idsObj instanceof List<?>) {
            return ((List<?>) idsObj).stream()
                    .filter(Objects::nonNull)
                    .map(obj -> Long.parseLong(obj.toString()))
                    .collect(Collectors.toList());
        } else if (idsObj == null) {
            List<Long> ids = geofenceReadEntityRepository.findAllByCompanyId(companyId)
                    .stream()
                    .map(GeofenceReadEntity::getId)
                    .collect(Collectors.toList());
            return ids;
        }
        return Collections.emptyList();
    }

    private GeofenceResponse getGeofenceWithFallback(Long geofenceId, Long companyId) {
        String geofenceKey = "geofence:" + companyId + ":" + geofenceId;
        Optional<GeofenceResponse> cached = geofenceCacheDao.get(geofenceKey, GeofenceResponse.class);
        if (cached.isPresent()) return cached.get();

        return geofenceReadEntityRepository.findById(geofenceId).map(entity -> {
            GeofenceResponse response = toGeofenceResponse(entity);
            vehicleGeofenceEntityRepository.findFirstByGeofenceId(geofenceId)
                    .ifPresent(vg -> response.setVehicleId(vg.getVehicleId()));
            geofenceCacheDao.save(geofenceKey, response, GEOFENCE_CACHE_TTL_SECONDS);
            return response;
        }).orElse(null);
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
        return r;
    }

    private void detectGeofenceEvent(Position position, GeofenceResponse geofence, Long companyId, Set<UUID> excludedUsers) {
        boolean currentlyInside = geofenceEvaluator.isInside(geofence, position.getLatitude(), position.getLongitude());
        GeofenceStatus currentStatus = currentlyInside ? GeofenceStatus.INSIDE : GeofenceStatus.OUTSIDE;

        Optional<GeofenceStatus> previousStatusOpt = geofenceStatusCacheStore.get(companyId, position.getImei(), geofence.getId());
        geofenceStatusCacheStore.save(companyId, position.getImei(), geofence.getId(), currentStatus);

        if (previousStatusOpt.isEmpty() || previousStatusOpt.get() == GeofenceStatus.UNKNOWN) return;

        GeofenceStatus previousStatus = previousStatusOpt.get();
        GeofenceEventType eventType = null;

        if (previousStatus == GeofenceStatus.OUTSIDE && currentStatus == GeofenceStatus.INSIDE) {
            eventType = GeofenceEventType.ENTRY;
        } else if (previousStatus == GeofenceStatus.INSIDE && currentStatus == GeofenceStatus.OUTSIDE) {
            eventType = GeofenceEventType.EXIT;
        }

        if (eventType != null) {
            publishEvent(position, geofence, companyId, eventType, excludedUsers);
        }
    }

    private void publishEvent(Position position, GeofenceResponse geofence, Long companyId, GeofenceEventType eventType, Set<UUID> excludedUsers) {
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

        VehicleGeofenceEntity geofenceConfig = (geofence.getVehicleId() != null)
                ? vehicleGeofenceEntityRepository.findByVehicleIdAndGeofenceId(geofence.getVehicleId(), geofence.getId()).orElse(null)
                : vehicleGeofenceEntityRepository.findFirstByGeofenceId(geofence.getId()).orElse(null);

        ApplicationEventCreate appEvent = GeofenceApplicationEventFactory.forGeofenceEvent(eventDto, geofenceConfig, excludedUsers);
        try {
            EventEntity eventEntity = EventEntity.map(appEvent);
            eventEntity = eventEntityRepository.save(eventEntity);
            eventResolver.resolveEvent(eventEntity);
        } catch (Exception e) {
            log.error("Error persistiendo evento de geocerca IMEI={}: {}", position.getImei(), e.getMessage());
        }
    }
}
