package com.ingestion.pe.mscore.domain.devices.app.handlers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.clients.cache.store.DeviceCacheStore;
import com.ingestion.pe.mscore.clients.models.DeviceResponse;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.config.cache.store.RedisPositionStore;
import com.ingestion.pe.mscore.domain.devices.app.batch.HistoricalBatchSaver;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import com.ingestion.pe.mscore.domain.atu.app.dispatcher.AtuTransmissionAsyncDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simula posiciones con aislamiento total de Kafka real.
 */
@SpringBootTest
@EnableScheduling
@TestPropertySource(properties = {
    "PROFILE_ACTIVE=test",
    "spring.profiles.active=test",
    "kafka.topic.position=test-position-topic",
    "kafka.topic.devices=test-devices-topic",
    "kafka.topic.websocket=test-websocket-topic",
    "kafka.topic.notifications=test-notifications-topic",
    "kafka.group.id=test-group-id",
    "spring.kafka.listener.auto-startup=false", 
    "spring.main.allow-bean-definition-overriding=true",
    "historical.batch.initial-delay=0",
    "historical.batch.fixed-delay=100"
})
class DeviceIngestionE2ETest {

    @Autowired
    private DeviceServiceHandler deviceServiceHandler;

    @Autowired
    private HistoricalBatchSaver historicalBatchSaver;

    @MockBean
    private DeviceEntityRepository deviceRepo;

    @MockBean
    private HistoricalDeviceEntityRepository historicalRepo;

    @MockBean
    private DeviceCacheStore cacheStore;

    @MockBean
    private RedisPositionStore redisStore;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private AtuTransmissionAsyncDispatcher atuDispatcher;

    @MockBean
    private PositionMonitoringAsyncDispatcher positionMonitoringAsyncDispatcher;

    @MockBean
    private AlertsAsyncDispatcher alertsAsyncDispatcher;

    private static final int TOTAL_POSITIONS = 100;
    private static final String TEST_IMEI = "860693082627994";

    @BeforeEach
    void setUp() {
        // Reset de mocks
        reset(kafkaTemplate, historicalRepo, deviceRepo);

        // Mock de dispositivo en caché 
        DeviceResponse mockResponse = new DeviceResponse();
        mockResponse.setId(37L);
        mockResponse.setImei(TEST_IMEI);
        mockResponse.setCompanyId(10L);
        mockResponse.setSensor(new HashMap<>());
        mockResponse.setSensorRaw(new HashMap<>());
        mockResponse.setDataHistory(new ArrayList<>());
        mockResponse.setSensorData(Collections.emptyList());
        
        when(cacheStore.getByImei(TEST_IMEI)).thenReturn(Optional.of(mockResponse));
        
        // Simular que el saveAll retorna la lista
        when(historicalRepo.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void testHighThroughputIngestionFlow() throws InterruptedException {
        // Usamos un pool más grande para simular concurrencia masiva real
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(TOTAL_POSITIONS);
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println(">>> INICIANDO INGESTA DE " + TOTAL_POSITIONS + " POSICIONES...");
        long startTime = System.nanoTime();

        for (int i = 0; i < TOTAL_POSITIONS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Position pos = createMockPosition(index);
                    deviceServiceHandler.handleDeviceEvent(pos);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("ERROR en procesamiento: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        long totalDurationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        System.out.println(">>> RESUMEN FAST-LANE:");
        System.out.println("  - Procesadas: " + successCount.get());
        System.out.println("  - Tiempo Total: " + totalDurationMs + "ms");
        System.out.println("  - Latencia Promedio: " + (totalDurationMs / (double)TOTAL_POSITIONS) + "ms");

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(TOTAL_POSITIONS);

        verify(kafkaTemplate, times(TOTAL_POSITIONS)).send(any(), any());

        System.out.println(">>> ESPERANDO VACIADO DE BATCH (SLOW-LANE)...");
        
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(historicalBatchSaver.getPendingCount()).isEqualTo(0);
                    verify(historicalRepo, atLeastOnce()).saveAll(anyList());
                });

        System.out.println(">>> [OK] Test de Performance superado.");
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    private Position createMockPosition(int index) {
        Position p = new Position();
        p.setImei(TEST_IMEI);
        p.setLatitude(-12.000 + (index * 0.0001));
        p.setLongitude(-77.000 + (index * 0.0001));
        p.setSpeedInKm(55.0);
        p.setDeviceTime(new Date());
        p.setCorrelationId(java.util.UUID.randomUUID().toString());
        p.setAttributes(new HashMap<>());
        return p;
    }
}
