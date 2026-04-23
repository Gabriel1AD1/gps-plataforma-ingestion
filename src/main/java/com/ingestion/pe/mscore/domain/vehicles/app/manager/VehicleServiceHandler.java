package com.ingestion.pe.mscore.domain.vehicles.app.manager;

import com.ingestion.pe.mscore.applications.tracking.DistanceCalculator;
import com.ingestion.pe.mscore.bridge.pub.service.KafkaPublisherService;
import com.ingestion.pe.mscore.clients.cache.store.DeviceCacheStore;
import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.domain.devices.app.handlers.models.StatusDevice;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.vehicles.app.factory.VehicleWebsocketMessageFactory;
import com.ingestion.pe.mscore.domain.vehicles.core.dto.response.VehicleStatusSummary;
import com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleTrackingEntity;
import com.ingestion.pe.mscore.domain.vehicles.core.enums.IgnitionStatus;
import com.ingestion.pe.mscore.domain.vehicles.core.enums.VehicleStatus;
import com.ingestion.pe.mscore.domain.vehicles.core.models.SnapshotManager;
import com.ingestion.pe.mscore.domain.vehicles.core.repo.VehicleTrackingEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceHandler {

    private final VehicleTrackingEntityRepository vehicleTrackingEntityRepository;
    private final KafkaPublisherService kafkaPublisherService;
    private final DistanceCalculator distanceCalculator;
    private final DeviceCacheStore deviceCacheStore;

    public void processPositionForVehicle(Position position, DeviceEntity device) {
        log.debug("Procesando actualización de vehículo para deviceId: {}", device.getId());
        
        Optional<VehicleTrackingEntity> vehicleOpt = vehicleTrackingEntityRepository.findByDeviceId(device.getId());
        
        if (vehicleOpt.isPresent()) {
            VehicleTrackingEntity vehicle = vehicleOpt.get();
            
            IgnitionStatus ignitionStatus = position.getIgnition() != null && position.getIgnition().equals(1) ? IgnitionStatus.on : IgnitionStatus.off;

            SnapshotManager snapshotManager = SnapshotManager.of(
                position.getCorrelationId(),
                VehicleStatus.online,
                position.getSatellites(),
                position.getHorizontalDilutionOfPrecision(),
                ignitionStatus,
                position.getSpeedInKm(),
                position.getLatitude(),
                position.getLongitude(),
                position.getAltitude(),
                position.getDeviceTime() != null ? position.getDeviceTime().toInstant() : Instant.now()
            );

            double currentOdometer = vehicle.getOdometerKm() != null ? vehicle.getOdometerKm() : 0.0;
            log.info("DEBUG-ODOMETER: Valor base traído de la base de datos para vehicle {} es: {}", vehicle.getId(), currentOdometer);

            if (vehicle.getSnapshotManager() != null && position.isValid()) {
                double delta = distanceCalculator.calculateDistance(
                        vehicle.getSnapshotManager().getLatitude(),
                        vehicle.getSnapshotManager().getLongitude(),
                        position.getLatitude(),
                        position.getLongitude()
                );
                currentOdometer += delta;
                log.info("DEBUG-ODOMETER: Delta calculado por Haversine: +{} km, Total actual: {} km", delta, currentOdometer);
            } else {
                log.info("DEBUG-ODOMETER: No se calculó delta. Snapshot nulo o posición no válida.");
            }

            vehicle.setOdometerKm(currentOdometer);
            
            vehicle.setStatus(VehicleStatus.online);
            vehicle.setSensors(device.getSensor());
            vehicle.setUpdated(Instant.now());
            vehicle.addSnapshotManager(snapshotManager);
            
            vehicleTrackingEntityRepository.save(vehicle);
            
            log.debug("Publicando VEHICLE_UPDATE para vehicleId: {}", vehicle.getId());
            kafkaPublisherService.publishWebsocketMessage(
                VehicleWebsocketMessageFactory.newVehicleUpdate(device, vehicle)
            );
        } else {
            log.debug("No se encontró vehículo asociado al deviceId: {}", device.getId());
        }
    }

    public void processStatusForVehicle(StatusDevice statusDevice) {
        log.debug("Procesando actualización de ESTADO de vehículo para deviceId: {}", statusDevice.getDeviceId());

        List<VehicleTrackingEntity> vehicles = vehicleTrackingEntityRepository.findAllByDeviceId(statusDevice.getDeviceId());
        
        if (vehicles.isEmpty()) {
            log.debug("No se encontró vehículo para cambiar de estado (deviceId: {})", statusDevice.getDeviceId());
            return;
        }

        VehicleStatus newStatus = switch (statusDevice.getStatus()) {
            case online -> VehicleStatus.online;
            case offline -> VehicleStatus.offline;
            default -> VehicleStatus.unknown;
        };

        for (VehicleTrackingEntity vehicle : vehicles) {
            vehicle.setStatus(newStatus);
            vehicle.setUpdated(Instant.now());
            vehicleTrackingEntityRepository.save(vehicle);
            
            log.debug("Publicando VEHICLE_STATUS_UPDATE para vehicleId: {}", vehicle.getId());
            kafkaPublisherService.publishWebsocketMessage(
                VehicleWebsocketMessageFactory.newVehicleStatus(vehicle)
            );
            
            publishVehicleStatusSummary(vehicle);
        }
    }

    private void publishVehicleStatusSummary(VehicleTrackingEntity vehicle) {
        Long companyId = vehicle.getCompanyId();
        if (companyId == null) return;
        List<VehicleStatusSummary> summaries = vehicleTrackingEntityRepository.countSummaryByCompanyId(companyId);
        kafkaPublisherService.publishWebsocketMessage(
            VehicleWebsocketMessageFactory.newVehicleStatusSummary(vehicle, summaries)
        );
    }
}
