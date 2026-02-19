package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoricalDeviceEntityRepository
        extends JpaRepository<HistoricalDeviceEntity, Long> {
}
