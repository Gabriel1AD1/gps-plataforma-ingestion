package com.ingestion.pe.mscore.domain.drivers.core.repo;

import com.ingestion.pe.mscore.domain.drivers.core.entity.DriverReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverReadEntityRepository extends JpaRepository<DriverReadEntity, Long> {
}
