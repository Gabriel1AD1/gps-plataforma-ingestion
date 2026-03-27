package com.ingestion.pe.mscore.domain.dispatch.core.repo;

import com.ingestion.pe.mscore.domain.dispatch.core.entity.TripReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripReadEntityRepository extends JpaRepository<TripReadEntity, Long> {
    List<TripReadEntity> findAllByRouteIdAndStatus(Long routeId, String status);
    Optional<TripReadEntity> findFirstByVehicleIdAndStatus(Long vehicleId, String status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t.routeId FROM TripReadEntity t WHERE t.status = 'ACTIVE'")
    List<Long> findAllActiveRouteIds();
}
