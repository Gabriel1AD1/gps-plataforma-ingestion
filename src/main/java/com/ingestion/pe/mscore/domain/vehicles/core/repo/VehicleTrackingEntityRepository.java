package com.ingestion.pe.mscore.domain.vehicles.core.repo;

import com.ingestion.pe.mscore.domain.vehicles.core.entity.VehicleTrackingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import com.ingestion.pe.mscore.domain.vehicles.core.dto.response.VehicleStatusSummary;

public interface VehicleTrackingEntityRepository extends JpaRepository<VehicleTrackingEntity, Long> {
    List<VehicleTrackingEntity> findAllByDeviceId(Long deviceId);
    Optional<VehicleTrackingEntity> findByDeviceId(Long deviceId);

    @Query("SELECT new com.ingestion.pe.mscore.domain.vehicles.core.dto.response.VehicleStatusSummary(COUNT(v.id), v.status) " +
           "FROM VehicleTrackingEntity v WHERE v.companyId = :companyId GROUP BY v.status")
    List<VehicleStatusSummary> countSummaryByCompanyId(@Param("companyId") Long companyId);
}
