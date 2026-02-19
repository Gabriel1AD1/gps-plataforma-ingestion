package com.ingestion.pe.mscore.clients;

import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VehicleClient {
    public List<VehicleResponse> getVehiclesByIds(List<Long> ids) {
        return Collections.emptyList();
    }
}
