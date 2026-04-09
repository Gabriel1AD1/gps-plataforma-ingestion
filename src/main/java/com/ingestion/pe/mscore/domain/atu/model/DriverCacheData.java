package com.ingestion.pe.mscore.domain.atu.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *   driver:data:{driverId}
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverCacheData implements Serializable {
    private Long id;
    private Long companyId;
    private String name;
    private String lastName;
    private String documentNumber;
}
