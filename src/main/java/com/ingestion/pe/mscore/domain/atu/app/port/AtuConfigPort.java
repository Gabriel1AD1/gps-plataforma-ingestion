package com.ingestion.pe.mscore.domain.atu.app.port;

import com.ingestion.pe.mscore.domain.atu.model.AtuTokenCache;
import java.util.Optional;

public interface AtuConfigPort {

    Optional<AtuTokenCache> getAtuConfig(Long companyId);
}
