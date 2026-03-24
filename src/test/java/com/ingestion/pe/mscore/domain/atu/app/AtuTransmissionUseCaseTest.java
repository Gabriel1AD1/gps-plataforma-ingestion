package com.ingestion.pe.mscore.domain.atu.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.atu.core.model.AtuPayload;
import com.ingestion.pe.mscore.domain.atu.model.AtuTokenCache;
import com.ingestion.pe.mscore.domain.atu.app.port.AtuConfigPort;
import com.ingestion.pe.mscore.domain.atu.app.port.AtuWebSocketPort;
import com.ingestion.pe.mscore.domain.atu.app.port.DriverDataPort;
import com.ingestion.pe.mscore.domain.atu.app.service.AtuTransmissionUseCase;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AtuTransmissionUseCaseTest {

    @Mock private AtuConfigPort atuConfigPort;
    @Mock private DriverDataPort driverDataPort;
    @Mock private AtuWebSocketPort atuWebSocketPort;
    @Mock private TripStateManager tripStateManager;
    @Mock private RouteConfigClient routeConfigClient;

    @InjectMocks
    private AtuTransmissionUseCase atuTransmissionUseCase;

    private Position position;
    private Long deviceId = 100L;
    private Long companyId = 200L;
    private Long vehicleId = 300L;

    @BeforeEach
    void setUp() {
        position = new Position();
        position.setImei("123456789012345");
        position.setLatitude(-12.0);
        position.setLongitude(-77.0);
        position.setSpeedInKm(45.5);
        position.setDeviceTime(new Date());
    }

    @Test
    void testCompanyWithoutToken_ShouldSkip() {
        when(atuConfigPort.getAtuConfig(companyId)).thenReturn(Optional.empty());

        atuTransmissionUseCase.evaluateAndTransmit(position, deviceId, companyId);

        verify(atuWebSocketPort, never()).sendPayload(any(), any(), any());
    }

    @Test
    void testDeviceWithoutActiveTrip_ShouldSkip() {
        when(atuConfigPort.getAtuConfig(companyId)).thenReturn(Optional.of(new AtuTokenCache("token", "ws://url")));
        when(routeConfigClient.getVehicleIdByDeviceId(deviceId)).thenReturn(Optional.of(vehicleId));
        when(tripStateManager.getStateByVehicleId(vehicleId)).thenReturn(Optional.empty());

        atuTransmissionUseCase.evaluateAndTransmit(position, deviceId, companyId);

        verify(atuWebSocketPort, never()).sendPayload(any(), any(), any());
    }

    @Test
    void testStaleTrama_ShouldSkip() {
        when(atuConfigPort.getAtuConfig(companyId)).thenReturn(Optional.of(new AtuTokenCache("token", "ws://url")));
        when(routeConfigClient.getVehicleIdByDeviceId(deviceId)).thenReturn(Optional.of(vehicleId));
        when(tripStateManager.getStateByVehicleId(vehicleId)).thenReturn(Optional.of(new TripState()));

        position.setDeviceTime(Date.from(Instant.now().minus(15, ChronoUnit.MINUTES)));

        atuTransmissionUseCase.evaluateAndTransmit(position, deviceId, companyId);

        verify(atuWebSocketPort, never()).sendPayload(any(), any(), any());
    }

    @Test
    void testValidTransmission_ShouldConstructCorrectPayload() {
        AtuTokenCache tokenCache = new AtuTokenCache("MY_TOKEN", "ws://atu");
        when(atuConfigPort.getAtuConfig(companyId)).thenReturn(Optional.of(tokenCache));
        when(routeConfigClient.getVehicleIdByDeviceId(deviceId)).thenReturn(Optional.of(vehicleId));

        TripState tripState = new TripState();
        tripState.setRouteId(500L);
        tripState.setDirection("OUTBOUND");
        tripState.setDriverId(900L);
        tripState.setDispatchTime(Instant.now().minus(5, ChronoUnit.MINUTES));
        when(tripStateManager.getStateByVehicleId(vehicleId)).thenReturn(Optional.of(tripState));

        RouteConfigResponse routeConfig = new RouteConfigResponse();
        routeConfig.setAtuRouteCode("ATU-1180");
        when(routeConfigClient.getRouteConfig(500L)).thenReturn(Optional.of(routeConfig));

        when(routeConfigClient.getVehiclePlate(vehicleId)).thenReturn(Optional.of("ABC-123"));
        when(driverDataPort.getDriverDocumentNumber(900L)).thenReturn(Optional.of("77665544"));

        atuTransmissionUseCase.evaluateAndTransmit(position, deviceId, companyId);

        ArgumentCaptor<AtuPayload> payloadCaptor = ArgumentCaptor.forClass(AtuPayload.class);
        verify(atuWebSocketPort).sendPayload(eq("MY_TOKEN"), eq("ws://atu"), payloadCaptor.capture());

        AtuPayload payload = payloadCaptor.getValue();
        assertEquals("123456789012345", payload.getImei());
        assertEquals("ATU-1180", payload.getRouteId());
        assertEquals("ABC-123", payload.getLicensePlate());
        assertEquals(0, payload.getDirectionId());
        assertEquals("77665544", payload.getDriverId());
        assertNotNull(payload.getIdentifier());
    }
}
