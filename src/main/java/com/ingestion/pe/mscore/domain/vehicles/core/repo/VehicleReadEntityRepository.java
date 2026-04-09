package com.ingestion.pe.mscore.domain.vehicles.core.repo;

import com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VehicleReadEntityRepository extends JpaRepository<VehicleReadEntity, Long> {
    Optional<VehicleReadEntity> findByDeviceId(Long deviceId);
}
