package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceConfigAlertsEntityRepository extends JpaRepository<DeviceConfigAlertsEntity, Long> {
}
