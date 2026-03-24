package com.ingestion.pe.mscore.applications.tracking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ingestion.pe.mscore.applications.tracking.enums.GeofenceEventType;
import com.ingestion.pe.mscore.applications.tracking.enums.GeofenceStatus;
import com.ingestion.pe.mscore.bridge.pub.models.GeofenceEventDto;
import com.ingestion.pe.mscore.bridge.pub.service.KafkaBusinessEventPublisher;
import com.ingestion.pe.mscore.commons.models.GeofenceResponse;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.config.cache.store.GeofenceStatusCacheStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingProcessorService - Unit Tests")
class TrackingProcessorServiceTest {

    @Mock
    private DistanceCalculator distanceCalculator;
    @Mock
    private GeofenceEvaluator geofenceEvaluator;
    @Mock
    private GeofenceStatusCacheStore geofenceStatusCacheStore;
    @Mock
    private KafkaBusinessEventPublisher kafkaBusinessEventPublisher;
    @Mock
    private CacheDao<GeofenceResponse> geofenceCacheDao;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TrackingProcessorService trackingProcessorService;

    private Position mockPosition;
    private GeofenceResponse mockGeofence;
    private final Long COMPANY_ID = 200L;

    @BeforeEach
    void setUp() {
        mockPosition = new Position();
        mockPosition.setImei("TEST-IMEI-123");
        mockPosition.setLatitude(12.34);
        mockPosition.setLongitude(56.78);
        mockPosition.setDeviceTime(java.util.Date.from(java.time.Instant.now()));

        mockGeofence = new GeofenceResponse();
        mockGeofence.setId(10L);
        mockGeofence.setName("ZONA_TEST");
    }

    @Test
    @DisplayName("Debe retornar inmediatamente cuando los parámetros son nulos")
    void testProcessPosition_InvalidParams() {
        trackingProcessorService.processPositionForTracking(null, COMPANY_ID);
        trackingProcessorService.processPositionForTracking(mockPosition, null);

        verify(distanceCalculator, never()).updateDistance(any(), any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Si no hay geocercas en Redis, solo debe actualizar la distancia")
    void testProcessPosition_NoGeofences() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("geofence:company:" + COMPANY_ID)).thenReturn(null);

        trackingProcessorService.processPositionForTracking(mockPosition, COMPANY_ID);

        verify(distanceCalculator, times(1)).updateDistance(mockPosition, COMPANY_ID);
        verify(geofenceCacheDao, never()).get(anyString(), any());
    }

    @Test
    @DisplayName("Detecta correctamente un evento de entrada")
    void testProcessPosition_DetectsEntryEvent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("geofence:company:" + COMPANY_ID)).thenReturn(List.of(10L));

        when(geofenceCacheDao.get("geofence:" + COMPANY_ID + ":10", GeofenceResponse.class))
                .thenReturn(Optional.of(mockGeofence));

        when(geofenceEvaluator.isInside(mockGeofence, mockPosition.getLatitude(), mockPosition.getLongitude()))
                .thenReturn(true);
        when(geofenceStatusCacheStore.get(COMPANY_ID, mockPosition.getImei(), 10L))
                .thenReturn(Optional.of(GeofenceStatus.OUTSIDE));

        trackingProcessorService.processPositionForTracking(mockPosition, COMPANY_ID);

        verify(geofenceStatusCacheStore, times(1)).save(COMPANY_ID, mockPosition.getImei(), 10L, GeofenceStatus.INSIDE);
        verify(kafkaBusinessEventPublisher, times(1)).publishGeofenceEvent(any(GeofenceEventDto.class));
    }

    @Test
    @DisplayName("Detecta correctamente un evento de salida")
    void testProcessPosition_DetectsExitEvent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("geofence:company:" + COMPANY_ID)).thenReturn(List.of("10"));

        when(geofenceCacheDao.get("geofence:" + COMPANY_ID + ":10", GeofenceResponse.class))
                .thenReturn(Optional.of(mockGeofence));

        when(geofenceEvaluator.isInside(mockGeofence, mockPosition.getLatitude(), mockPosition.getLongitude()))
                .thenReturn(false);
        when(geofenceStatusCacheStore.get(COMPANY_ID, mockPosition.getImei(), 10L))
                .thenReturn(Optional.of(GeofenceStatus.INSIDE));

        trackingProcessorService.processPositionForTracking(mockPosition, COMPANY_ID);

        verify(geofenceStatusCacheStore, times(1)).save(COMPANY_ID, mockPosition.getImei(), 10L,
                GeofenceStatus.OUTSIDE);
        verify(kafkaBusinessEventPublisher, times(1)).publishGeofenceEvent(any(GeofenceEventDto.class));
    }

    @Test
    @DisplayName("Permanece dentro de la geocerca, no se genera evento")
    void testProcessPosition_RemainsInside() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("geofence:company:" + COMPANY_ID)).thenReturn(List.of(10L));

        when(geofenceCacheDao.get("geofence:" + COMPANY_ID + ":10", GeofenceResponse.class))
                .thenReturn(Optional.of(mockGeofence));

        when(geofenceEvaluator.isInside(mockGeofence, mockPosition.getLatitude(), mockPosition.getLongitude()))
                .thenReturn(true);
        when(geofenceStatusCacheStore.get(COMPANY_ID, mockPosition.getImei(), 10L))
                .thenReturn(Optional.of(GeofenceStatus.INSIDE));

        trackingProcessorService.processPositionForTracking(mockPosition, COMPANY_ID);

        verify(geofenceStatusCacheStore, times(1)).save(COMPANY_ID, mockPosition.getImei(), 10L, GeofenceStatus.INSIDE);
        verify(kafkaBusinessEventPublisher, never()).publishGeofenceEvent(any());
    }
}
