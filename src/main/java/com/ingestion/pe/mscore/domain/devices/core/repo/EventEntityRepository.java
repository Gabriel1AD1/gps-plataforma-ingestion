package com.ingestion.pe.mscore.domain.devices.core.repo;

import com.ingestion.pe.mscore.domain.devices.core.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventEntityRepository extends JpaRepository<EventEntity, Long> {

  Optional<EventEntity> findTopByAggregateIdAndEventTypeAndResolvedFalseOrderByOccurredAtDesc(
      String aggregateId, String eventType);

  boolean existsByAggregateIdAndEventTypeAndResolvedFalse(String aggregateId, String eventType);

}
