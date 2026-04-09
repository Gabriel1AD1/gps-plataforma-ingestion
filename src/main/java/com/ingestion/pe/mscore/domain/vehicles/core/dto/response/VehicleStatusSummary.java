package com.ingestion.pe.mscore.domain.vehicles.core.dto.response;

import com.ingestion.pe.mscore.domain.vehicles.core.enums.VehicleStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class VehicleStatusSummary {
  private Long count;
  private VehicleStatus status;

  public VehicleStatusSummary(Long count, VehicleStatus status) {
    this.count = count;
    this.status = status;
  }
}
