package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.enums.DeviceStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceEntityRepository extends JpaRepository<DeviceEntity, Long> {

    // @EntityGraph(attributePaths = {"overrideSensors"}) // Comentado temporalmente
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
}
