package com.ingestion.pe.mscore.domain.atu.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *   atu:company:token:{companyId}
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AtuTokenCache implements Serializable {
    private String token;
    private String endpoint;
}
