package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.OverrideSensorsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OverrideSensorsEntityRepository extends JpaRepository<OverrideSensorsEntity, Long> {
    List<OverrideSensorsEntity> findAllByCompanyId(Long companyId);

    @Query("select o from OverrideSensorsEntity o where o.id = ?1 and o.companyId = ?2")
    OverrideSensorsEntity findByIdAndCompanyId(Long id, Long companyId);

    OverrideSensorsEntity getReferenceByIdAndCompanyId(Long id, Long companyId);

    @Query("""
        SELECT o
        FROM OverrideSensorsEntity o
        WHERE o.companyId = :companyId
          AND (:title IS NULL OR :title = '' OR o.title LIKE %:title%)
          AND (:description IS NULL OR :description = '' OR o.description LIKE %:description%)
          AND (:maxDevicesUsed IS NULL OR o.maxDevicesUsed = :maxDevicesUsed)
        """)
    Page<OverrideSensorsEntity> findAllSearch(
        @Param("companyId") Long companyId,
        @Param("title") String title,
        @Param("description") String description,
        @Param("maxDevicesUsed") Long maxDevicesUsed,
        Pageable pageable
    );
}

