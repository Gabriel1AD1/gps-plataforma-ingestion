package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/*
 * Stub repository for UserDeviceEntity to allow compilation.
 * Ingestion does not need user associations yet, but the handler logic references it.
 * This should be refactored later to meaningful logic if needed.
 */
@Repository
public class UserDeviceEntityRepository {
    public Set<UUID> findAllByDeviceReturnUuids(DeviceEntity device) {
        return Set.of();
    }
}
