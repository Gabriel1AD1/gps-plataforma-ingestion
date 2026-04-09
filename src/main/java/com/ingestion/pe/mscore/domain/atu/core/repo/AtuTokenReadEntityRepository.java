package com.ingestion.pe.mscore.domain.atu.core.repo;

import com.ingestion.pe.mscore.domain.atu.core.entity.AtuTokenReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AtuTokenReadEntityRepository extends JpaRepository<AtuTokenReadEntity, Long> {
    Optional<AtuTokenReadEntity> findByCompanyId(Long companyId);
}
