package com.ingestion.pe.mscore.e2e;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.config.cache.store.RedisPositionStore;
import com.ingestion.pe.mscore.config.kafka.sub.KafkaSubscriptionPositions;
import com.ingestion.pe.mscore.config.kafka.sub.KafkaSubscriptionStatusDevice;
import com.ingestion.pe.mscore.domain.atu.app.service.AtuTransmissionUseCase;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceStatus;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.EventEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
@Transactional
class DeviceIngestionE2EIntegrationTest {

    @Autowired private KafkaSubscriptionPositions positionsListener;
    @Autowired private KafkaSubscriptionStatusDevice statusListener;
    @Autowired private DeviceEntityRepository deviceRepo;
    @Autowired private HistoricalDeviceEntityRepository historicalRepo;
    @Autowired private EventEntityRepository eventRepo;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockBean private KafkaPublisherService kafkaPublisher;
    @MockBean private RedisPositionStore redisPositionStore;

    @SpyBean private AtuTransmissionUseCase atuTransmissionUseCase;

    private final String TEST_IMEI = "000000000000000";
    private DeviceEntity device;

    @BeforeEach
    void setUp() {
        // Limpiar base de datos
        deviceRepo.deleteAll();
        historicalRepo.deleteAll();
        eventRepo.deleteAll();
        
        // Limpiar tablas de geocercas
        jdbcTemplate.execute("DELETE FROM vehicle_module.vehicle_geofence");
        jdbcTemplate.execute("DELETE FROM geofences_module.geofences");

        // dispositivo
        device = new DeviceEntity();
        device.setImei(TEST_IMEI);
        device.setSerialNumber("SN-001");
        device.setLatitude(0.0);
        device.setLongitude(0.0);
        device.setOdometerInMeters(0L);
        device.setCompany(1L);
        device.setDeviceStatus(DeviceStatus.offline);
        device.setDeviceType(com.ingestion.pe.mscore.domain.devices.core.enums.DeviceType.BUS);
        device.setBrand("COBAN");
        device.setModel("TK103");
        device.setFirmwareVersion("1.0");
        device.setPassword("123456");
        device = deviceRepo.saveAndFlush(device);

        jdbcTemplate.execute("INSERT INTO geofences_module.geofences (id, company_id, name, latitude_center, longitude_center, radius_in_meters, type) " +
                           "VALUES (100, 1, 'Test Geofence', -12.0463, -77.0427, 500.0, 'circle')");

        jdbcTemplate.execute("INSERT INTO vehicle_module.vehicle_geofence (id, vehicle_id, geofence_id, active) " +
                           "VALUES (200, 1, 100, true)");
    }

    @Test
    @DisplayName("Flujo E2E Completo: Status -> Posicion -> Geocerca -> ATU")
    void testFullIngestionFlowE2E() {
        // dispositivo (online)
        String statusJson = "{\"deviceId\":" + device.getId() + ", \"status\":\"online\", \"resetDevices\":false}";
        statusListener.listen(statusJson);

        DeviceEntity updatedStatus = deviceRepo.findById(device.getId()).orElseThrow();
        assertThat(updatedStatus.getDeviceStatus()).isEqualTo(DeviceStatus.online);

        List<String> positionBatch = Arrays.asList(
            "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-11.0, \"longitude\":-76.0, \"speed\":10, \"device_time\":\"2026-03-28T12:00:01Z\"}",
            "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-12.0463, \"longitude\":-77.0427, \"speed\":20, \"device_time\":\"2026-03-28T12:00:05Z\"}",
            "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-13.0, \"longitude\":-78.0, \"speed\":30, \"device_time\":\"2026-03-28T12:00:10Z\"}"
        );

        positionsListener.listen(positionBatch);
        
        assertThat(historicalRepo.count()).isEqualTo(3);

        DeviceEntity finalDevice = deviceRepo.findById(device.getId()).orElseThrow();
        assertThat(finalDevice.getOdometerInMeters()).isGreaterThan(0);
        assertThat(finalDevice.getLatitude()).isEqualTo(-13.0);
        assertThat(finalDevice.getSpeedInKmh()).isEqualTo(30.0);

        verify(atuTransmissionUseCase, times(3)).evaluateAndTransmit(any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Robustez: IMEI inexistente")
    void testUnknownImeiFlow() {
        String unknownImei = "999999999999999";
        List<String> positionBatch = Arrays.asList(
            "{\"imei\":\"" + unknownImei + "\", \"latitude\":-12.0, \"longitude\":-77.0, \"speed\":10, \"device_time\":\"2026-03-28T12:00:01Z\"}"
        );

        positionsListener.listen(positionBatch);

        assertThat(historicalRepo.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Robustez: JSON malformado")
    void testMalformedJson() {
        List<String> brokenBatch = Arrays.asList(
            "{\"imei\":\"" + TEST_IMEI + "\", \"latitude\":-12.0, \"broken_json: true}"
        );

        positionsListener.listen(brokenBatch);
        
        assertThat(historicalRepo.count()).isEqualTo(0);
    }
}
