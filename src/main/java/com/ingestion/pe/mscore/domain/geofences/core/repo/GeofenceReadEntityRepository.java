package com.ingestion.pe.mscore.domain.geofences.core.repo;

import com.ingestion.pe.mscore.domain.geofences.core.entity.GeofenceReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeofenceReadEntityRepository extends JpaRepository<GeofenceReadEntity, Long> {

    @Query("SELECT g FROM GeofenceReadEntity g WHERE g.companyId = :companyId")
    List<GeofenceReadEntity> findAllByCompanyId(@Param("companyId") Long companyId);
}
