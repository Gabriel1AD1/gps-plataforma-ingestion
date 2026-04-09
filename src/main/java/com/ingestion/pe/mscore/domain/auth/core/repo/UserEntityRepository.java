package com.ingestion.pe.mscore.domain.auth.core.repo;

import com.ingestion.pe.mscore.domain.auth.core.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {
}
