package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricalDeviceEntityRepository
        extends JpaRepository<HistoricalDeviceEntity, Long> {
}
