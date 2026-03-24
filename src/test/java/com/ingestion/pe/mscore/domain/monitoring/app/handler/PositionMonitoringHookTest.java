package com.ingestion.pe.mscore.domain.monitoring.app.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.commons.models.WebsocketMessage;
import com.ingestion.pe.mscore.domain.monitoring.core.calc.DelayCalculator;
import com.ingestion.pe.mscore.domain.monitoring.core.model.ControlPointModel;
import com.ingestion.pe.mscore.domain.monitoring.core.model.RouteConfigResponse;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import com.ingestion.pe.mscore.domain.monitoring.core.service.TripStateManager;
import com.ingestion.pe.mscore.domain.monitoring.infra.cache.RouteConfigClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionMonitoringHookTest {

    @Mock
    private TripStateManager tripStateManager;
    @Mock
    private RouteConfigClient routeConfigClient;
    @Mock
    private DelayCalculator delayCalculator;
    @Mock
    private KafkaPublisherService kafkaPublisherService;

    @InjectMocks
    private PositionMonitoringHook hook;

    private TripState existingState;
    private RouteConfigResponse routeConfig;

    @BeforeEach
    void setUp() {
        existingState = TripState.builder()
                .tripId(1L).routeId(10L).vehicleId(100L)
                .currentPointIndex(0)
                .accumulatedDistanceKm(0.0)
                .lastLatitude(-12.0).lastLongitude(-77.0)
                .lastUpdateTime(Instant.now().minus(30, ChronoUnit.SECONDS))
                .direction("OUTBOUND")
                .status("ON_TIME")
                .accumulatedDelayMinutes(0.0)
                .build();

        List<ControlPointModel> controlPoints = List.of(
                ControlPointModel.builder().id(10L).name("A")
                        .latitude(-12.0).longitude(-77.0)
                        .distanceFromStartKm(0.0).sequenceOrder(0).build(),
                ControlPointModel.builder().id(20L).name("B")
                        .latitude(-12.001).longitude(-77.001)
                        .distanceFromStartKm(0.14).sequenceOrder(1).build());

        routeConfig = RouteConfigResponse.builder()
                .controlPoints(controlPoints)
                .totalDistanceKm(0.14)
                .build();
    }

    @Test
    void onPositionReceived_noExistingState_noVehicle_doesNothing() {
        when(routeConfigClient.getVehicleIdByDeviceId(anyLong())).thenReturn(Optional.empty());

        hook.onPositionReceived(999L, -12.0, -77.0, 40.0, Instant.now());

        verify(tripStateManager, never()).updateState(anyLong(), any());
        verify(kafkaPublisherService, never()).publishWebsocketMessage(any());
    }

    @Test
    void onPositionReceived_existingState_updatesAndPublishes() {
        when(routeConfigClient.getVehicleIdByDeviceId(anyLong())).thenReturn(Optional.of(100L));
        when(tripStateManager.getStateByVehicleId(100L)).thenReturn(Optional.of(existingState));
        when(routeConfigClient.getRouteConfig(10L)).thenReturn(Optional.of(routeConfig));

        hook.onPositionReceived(1L, -12.0005, -77.0005, 45.0, Instant.now());

        verify(tripStateManager).updateState(eq(1L), any(TripState.class));
        verify(delayCalculator).evaluate(any(TripState.class), any(RouteConfigResponse.class));
        verify(kafkaPublisherService).publishWebsocketMessage(any(WebsocketMessage.class));
    }

    @Test
    void onPositionReceived_gpsAnomaly_discardsButDoesNotPublish() {
        existingState.setLastLatitude(-12.0);
        existingState.setLastLongitude(-77.0);
        existingState.setLastUpdateTime(Instant.now().minus(5, ChronoUnit.SECONDS));

        when(routeConfigClient.getVehicleIdByDeviceId(anyLong())).thenReturn(Optional.of(100L));
        when(tripStateManager.getStateByVehicleId(100L)).thenReturn(Optional.of(existingState));
        when(routeConfigClient.getRouteConfig(10L)).thenReturn(Optional.of(routeConfig));

        hook.onPositionReceived(1L, -17.0, -77.0, 50.0, Instant.now());

        verify(tripStateManager, never()).updateState(anyLong(), any());
        verify(kafkaPublisherService, never()).publishWebsocketMessage(any());
        verify(delayCalculator, never()).evaluate(any(), any());
    }

    @Test
    void onPositionReceived_noRouteConfig_doesNothing() {
        when(routeConfigClient.getVehicleIdByDeviceId(anyLong())).thenReturn(Optional.of(100L));
        when(tripStateManager.getStateByVehicleId(100L)).thenReturn(Optional.of(existingState));
        when(routeConfigClient.getRouteConfig(10L)).thenReturn(Optional.empty());

        hook.onPositionReceived(1L, -12.0005, -77.0005, 45.0, Instant.now());

        verify(tripStateManager, never()).updateState(anyLong(), any());
        verify(kafkaPublisherService, never()).publishWebsocketMessage(any());
    }

    @Test
    void onPositionReceived_websocketMessage_containsTripStateData() {
        when(routeConfigClient.getVehicleIdByDeviceId(anyLong())).thenReturn(Optional.of(100L));
        when(tripStateManager.getStateByVehicleId(100L)).thenReturn(Optional.of(existingState));
        when(routeConfigClient.getRouteConfig(10L)).thenReturn(Optional.of(routeConfig));

        hook.onPositionReceived(1L, -12.0005, -77.0005, 45.0, Instant.now());

        ArgumentCaptor<WebsocketMessage> captor = ArgumentCaptor.forClass(WebsocketMessage.class);
        verify(kafkaPublisherService).publishWebsocketMessage(captor.capture());

        WebsocketMessage msg = captor.getValue();
        assertEquals(WebsocketMessage.MessageAgregateType.TRIP_STATE_UPDATE, msg.getMessageAgregateType());
        assertEquals(WebsocketMessage.MessageType.REFRESH, msg.getMessageType());
        assertTrue(msg.getBroadcast());
        assertEquals(1L, msg.getProperties().get("tripId"));
        assertEquals(10L, msg.getProperties().get("routeId"));
        assertEquals(100L, msg.getProperties().get("vehicleId"));
    }

    @Test
    void onPositionReceived_controlPointCrossing_updatesIndex() {
        when(routeConfigClient.getVehicleIdByDeviceId(anyLong())).thenReturn(Optional.of(100L));
        when(tripStateManager.getStateByVehicleId(100L)).thenReturn(Optional.of(existingState));
        when(routeConfigClient.getRouteConfig(10L)).thenReturn(Optional.of(routeConfig));

        hook.onPositionReceived(1L, -12.001, -77.001, 50.0, Instant.now());

        ArgumentCaptor<TripState> stateCaptor = ArgumentCaptor.forClass(TripState.class);
        verify(tripStateManager).updateState(eq(1L), stateCaptor.capture());

        TripState updatedState = stateCaptor.getValue();
        assertEquals(1, updatedState.getCurrentPointIndex(), "Debe avanzar al punto B (index 1)");
    }

    @Test
    void onPositionReceived_hardwareAnomaly_discards() {
        when(routeConfigClient.getVehicleIdByDeviceId(anyLong())).thenReturn(Optional.of(100L));
        when(tripStateManager.getStateByVehicleId(100L)).thenReturn(Optional.of(existingState));

        // Velocidad de 180 km/h (Hardware error)
        hook.onPositionReceived(1L, -12.0005, -77.0005, 180.0, Instant.now());

        verify(tripStateManager, never()).updateState(anyLong(), any());
        verify(kafkaPublisherService, never()).publishWebsocketMessage(any());
    }
}
