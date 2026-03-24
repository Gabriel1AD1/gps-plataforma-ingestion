package com.ingestion.pe.mscore.domain.vehicles.core.repo;

import com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleGeofenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleGeofenceEntityRepository extends JpaRepository<VehicleGeofenceEntity, Long> {

    @Query("SELECT vg FROM VehicleGeofenceEntity vg WHERE vg.vehicleId = :vehicleId AND vg.geofenceId = :geofenceId")
    Optional<VehicleGeofenceEntity> findByVehicleIdAndGeofenceId(
            @Param("vehicleId") Long vehicleId,
            @Param("geofenceId") Long geofenceId);

    @Query("SELECT vg FROM VehicleGeofenceEntity vg WHERE vg.geofenceId = :geofenceId AND vg.active = true ORDER BY vg.id ASC")
    Optional<VehicleGeofenceEntity> findFirstByGeofenceId(@Param("geofenceId") Long geofenceId);
}
