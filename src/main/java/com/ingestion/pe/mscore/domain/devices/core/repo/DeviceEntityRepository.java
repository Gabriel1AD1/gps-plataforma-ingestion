package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.dto.response.DevicesStatusSummary;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceStatus;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceEntityRepository extends JpaRepository<DeviceEntity, Long> {

    @EntityGraph(attributePaths = { "overrideSensors" })
    Optional<DeviceEntity> findByImei(String imei);

    @Modifying
    @Transactional
    @Query("""
            update DeviceEntity d
            set
                d.deviceStatus = :status,

                d.lastConnection =
                    case
                        when :status = 'online' then CURRENT_TIMESTAMP
                        else d.lastConnection
                    end,

                d.lastDisconnection =
                    case
                        when :status = 'offline' then CURRENT_TIMESTAMP
                        else d.lastDisconnection
                    end,
                d.updated = CURRENT_TIMESTAMP
            """)
    void updateAllStatuses(@Param("status") DeviceStatus status);

    @Query("""
            SELECT new com.ingestion.pe.mscore.domain.devices.core.dto.response.DevicesStatusSummary(
              COUNT(d.id),
              d.deviceStatus
            )
            FROM DeviceEntity d
            WHERE d.company = :companyId
            GROUP BY d.deviceStatus
            """)
    List<DevicesStatusSummary> allSummary(@Param("companyId") Long companyId);

    @Modifying
    @Transactional
    @Query("UPDATE DeviceEntity d SET d.lastHistoricalDevice = :historicalId WHERE d.id = :deviceId")
    void updateLastHistoricalDevice(@Param("deviceId") Long deviceId, @Param("historicalId") Long historicalId);
}
