package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ingestion.pe.mscore.clients.VehicleClient;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.devices.app.manager.ManagerConfigAlert;
import com.ingestion.pe.mscore.domain.devices.app.resolver.EventResolver;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceConfigAlertsEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.UserDeviceEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class DeviceBatchIngestionTest {

    @Mock private DeviceEntityRepository deviceRepo;
    @Mock private HistoricalDeviceEntityRepository historicalRepo;
    @Mock private UserDeviceEntityRepository userDeviceRepo;
    @Mock private ManagerConfigAlert managerConfigAlert;
    @Mock private VehicleClient vehicleClient;
    @Mock private EventEntityRepository eventRepo;
    @Mock private EventResolver eventResolver;
    @Mock private DeviceConfigAlertsEntityRepository alertsRepo;
    @Mock private IndividualRecordFallbackService fallbackService;
    @Mock private PostPersistenceOrchestrator postPersistenceOrchestrator;

    @InjectMocks private DeviceBatchOrchestrator orchestrator;
    private DeviceBatchPersistenceService persistenceService;

    private DeviceEntity testDevice;
    private final String TEST_IMEI = "123456789012345";

    @BeforeEach
    void setUp() {
        persistenceService = new DeviceBatchPersistenceService(
                deviceRepo, historicalRepo, alertsRepo, eventRepo, fallbackService, postPersistenceOrchestrator
        );

        try {
            var field = DeviceBatchOrchestrator.class.getDeclaredField("persistenceService");
            field.setAccessible(true);
            field.set(orchestrator, persistenceService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testDevice = new DeviceEntity();
        testDevice.setId(1L);
        testDevice.setImei(TEST_IMEI);
        testDevice.setLatitude(-12.0);
        testDevice.setLongitude(-77.0);
        testDevice.setOdometerInMeters(1000L);
        testDevice.setCompany(10L);
    }

    @Test
    @DisplayName("Procesar batch completo en Bulk")
    void testHappyPathBulkIngestion() {
        List<String> messages = Arrays.asList(
            "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-12.1, \"longitude\":-77.1, \"speed\":50, \"deviceTime\":\"2026-03-28T10:00:00Z\"}",
            "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-12.2, \"longitude\":-77.2, \"speed\":60, \"deviceTime\":\"2026-03-28T10:01:00Z\"}"
        );

        when(deviceRepo.findByImeiIn(anySet())).thenReturn(Collections.singletonList(testDevice));
        when(userDeviceRepo.findExcludedUuidsByImeiIn(anySet())).thenReturn(new ArrayList<>());
        when(managerConfigAlert.executeConfigAlertRules(anyLong(), anyMap())).thenReturn(new HashSet<>());

        orchestrator.processBatch(messages);

        verify(deviceRepo, times(1)).findByImeiIn(anySet());
        verify(historicalRepo, times(1)).saveAll(anyList());
        verify(deviceRepo, times(1)).saveAll(anyList());
        verify(postPersistenceOrchestrator, times(1)).executePostPersistence(anyList());
        
        // Verificar que el kilometraje aumentó 
        assertThat(testDevice.getOdometerInMeters()).isGreaterThan(1000L);
    }

    @Test
    @DisplayName("Fallback - Fallo en Bulk activa procesamiento individual")
    void testSurgicalFallbackOnBulkFailure() {
        // Arrange
        List<String> messages = Collections.singletonList(
            "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-12.1, \"longitude\":-77.1, \"speed\":50, \"deviceTime\":\"2026-03-28T10:00:00Z\"}"
        );

        when(deviceRepo.findByImeiIn(anySet())).thenReturn(Collections.singletonList(testDevice));
        
        doThrow(new DataIntegrityViolationException("Duplicate key")).when(historicalRepo).saveAll(anyList());

        orchestrator.processBatch(messages);

        verify(fallbackService, times(1)).processSingleRecord(any());
        // Post-persistencia debe llamarse con los resultados exitosos del fallback
        verify(postPersistenceOrchestrator, times(1)).executePostPersistence(anyList());
    }

    @Test
    @DisplayName("Consistencia Cronologica - Batch desordenado se ordena correctamente")
    void testChronologicalConsistency() {
        String msgT2 = "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-12.2, \"longitude\":-77.2, \"deviceTime\":\"2026-03-28T10:05:00Z\"}";
        String msgT1 = "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-12.1, \"longitude\":-77.1, \"deviceTime\":\"2026-03-28T10:00:00Z\"}";
        List<String> messages = Arrays.asList(msgT2, msgT1);

        when(deviceRepo.findByImeiIn(anySet())).thenReturn(Collections.singletonList(testDevice));

        orchestrator.processBatch(messages);

        assertThat(testDevice.getDeviceTime()).isEqualTo(OffsetDateTime.parse("2026-03-28T10:05:00Z").toInstant());
        assertThat(testDevice.getLatitude()).isEqualTo(-12.2);
    }
}
