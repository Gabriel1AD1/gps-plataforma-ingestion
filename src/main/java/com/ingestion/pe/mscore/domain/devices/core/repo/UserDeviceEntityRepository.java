package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserDeviceEntityRepository {
    public Set<UUID> findAllByDeviceReturnUuids(DeviceEntity device) {
        return Set.of();
    }
}
