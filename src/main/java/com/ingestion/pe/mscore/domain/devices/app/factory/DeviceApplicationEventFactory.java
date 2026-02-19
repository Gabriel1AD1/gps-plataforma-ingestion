package com.ingestion.pe.mscore.domain.devices.app.factory;

import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceEntity;
import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeviceApplicationEventFactory {
    public static Object newConfigAlertNotResolved(
            DeviceEntity device,
            Set<UUID> usersNotSendPositions,
            Long companyId,
            DeviceConfigAlertsEntity triggeredAlertsFor,
            List<VehicleResponse> vehicleAsociateForDevice) {
        return "Stub Event";
    }
}
