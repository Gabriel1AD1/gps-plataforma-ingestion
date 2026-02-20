package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ingestion.pe.mscore.applications.tracking.TrackingProcessorService;
import com.ingestion.pe.mscore.bridge.pub.models.DevicePositionEventCreate;
import com.ingestion.pe.mscore.bridge.pub.service.EventCreateBridgeService;
import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.clients.VehicleClient;
import com.ingestion.pe.mscore.clients.cache.store.DeviceCacheStore;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.devices.app.manager.ManagerConfigAlert;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceConfigAlertsEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.UserDeviceEntityRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceHandlerTest {

    @Mock
    private DeviceEntityRepository deviceEntityRepository;
    @Mock
    private UserDeviceEntityRepository userDeviceEntityRepository;
    @Mock
    private KafkaPublisherService kafkaPublisherService;
    @Mock
    private HistoricalDeviceEntityRepository historicalDeviceEntityRepository;
    @Mock
    private DeviceConfigAlertsEntityRepository deviceConfigAlertsEntityRepository;
    @Mock
    private EventCreateBridgeService eventCreateBridgeService;
    @Mock
    private ManagerConfigAlert managerConfigAlert;
    @Mock
    private VehicleClient vehicleClient;
    @Mock
    private DeviceCacheStore deviceCacheStore;
    @Mock
    private TrackingProcessorService trackingProcessorService;

    @InjectMocks
    private DeviceServiceHandler deviceServiceHandler;

    private DeviceEntity deviceEntity;
    private Position position;

    @BeforeEach
    void setUp() {
        deviceEntity = new DeviceEntity();
        deviceEntity.setId(1L);
        deviceEntity.setImei("123456789012345");
        deviceEntity.setCompany(100L);
        deviceEntity.setSensorsData(new java.util.HashSet<>());
        deviceEntity.setDataHistory(new java.util.ArrayList<>());
        deviceEntity.setSensor(new java.util.HashMap<>());
        deviceEntity.setSensorRaw(new java.util.HashMap<>());
        deviceEntity.setSensorOnTime(new java.util.HashMap<>());

        position = new Position();
        position.setImei("123456789012345");
        position.setLatitude(10.0);
        position.setLongitude(20.0);
        position.setDeviceTime(java.util.Date.from(java.time.Instant.now()));
        position.setFixTime(java.util.Date.from(java.time.Instant.now()));
        position.setAttributes(new java.util.HashMap<>());
        position.setCorrelationId(java.util.UUID.randomUUID().toString());
    }

    @Test
    void handleDeviceEvent_ShouldProcessAndPublish() {
        // Arrange
        when(deviceEntityRepository.findByImei(anyString())).thenReturn(Optional.of(deviceEntity));
        when(historicalDeviceEntityRepository.save(any(HistoricalDeviceEntity.class)))
                .thenAnswer(invocation -> {
                    HistoricalDeviceEntity entity = invocation.getArgument(0);
                    entity.setId(500L);
                    return entity;
                });
        when(managerConfigAlert.executeConfigAlertRules(anyLong(), any())).thenReturn(Collections.emptySet());
        when(userDeviceEntityRepository.findAllByDeviceReturnUuids(any())).thenReturn(Collections.emptySet());

        // Act
        deviceServiceHandler.handleDeviceEvent(position);

        // Assert
        verify(deviceEntityRepository).save(deviceEntity);
        verify(historicalDeviceEntityRepository).save(any(HistoricalDeviceEntity.class));
        verify(kafkaPublisherService).publishWebsocketMessage(any(WebsocketMessage.class));
        verify(eventCreateBridgeService).createEvent(any(DevicePositionEventCreate.class));
        verify(deviceCacheStore).save(deviceEntity);
    }
}
