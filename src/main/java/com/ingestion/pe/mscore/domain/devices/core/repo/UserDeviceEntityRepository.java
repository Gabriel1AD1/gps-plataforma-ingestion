package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.UserDeviceExclusionsEntity;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDeviceEntityRepository extends JpaRepository<UserDeviceExclusionsEntity, Long> {
    
    @Query("select u.userUuid from UserDeviceExclusionsEntity u where u.device = ?1")
    Set<UUID> findAllByDeviceReturnUuids(DeviceEntity device);

    @Query("select u.userUuid from UserDeviceExclusionsEntity u where u.device.imei = ?1")
    Set<UUID> findExcludedUuidsByDeviceImei(String imei);

    @Query("select u.device.imei, u.userUuid from UserDeviceExclusionsEntity u where u.device.imei in ?1")
    List<Object[]> findExcludedUuidsByImeiIn(Collection<String> imeis);
}
