package com.ingestion.pe.mscore.domain.monitoring.core.service;

import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripStateManager {

    private static final String KEY_PREFIX = "trip:state:";
    private static final long TTL_SECONDS = 3600; // 1h backup

    private final ConcurrentHashMap<Long, TripState> activeStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> vehicleIdToTripId = new ConcurrentHashMap<>();
    private final CacheDao<TripState> tripStateCacheDao;

    // Actualizar estado de un viaje (llamado en cada posición GPS)
    public void updateState(Long tripId, TripState state) {
        activeStates.put(tripId, state);
        if (state.getVehicleId() != null) {
            vehicleIdToTripId.put(state.getVehicleId(), tripId);
        }
        try {
            tripStateCacheDao.save(KEY_PREFIX + tripId, state, TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Error guardando TripState {} en Redis: {}", tripId, e.getMessage());
        }
    }

    // Obtener estado de un viaje específico
    public Optional<TripState> getState(Long tripId) {
        return Optional.ofNullable(activeStates.get(tripId));
    }

    // Obtener todos los estados activos para una ruta
    public List<TripState> getStatesByRoute(Long routeId) {
        return activeStates.values().stream()
                .filter(state -> routeId.equals(state.getRouteId()))
                .toList();
    }

    // Remover estado al completar viaje
    public void removeState(Long tripId) {
        TripState removed = activeStates.remove(tripId);
        if (removed != null && removed.getVehicleId() != null) {
            vehicleIdToTripId.remove(removed.getVehicleId());
        }
        tripStateCacheDao.delete(KEY_PREFIX + tripId);
    }

    // Verificar si hay un estado activo para un vehículo
    public Optional<TripState> getStateByVehicleId(Long vehicleId) {
        Long tripId = vehicleIdToTripId.get(vehicleId);
        if (tripId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeStates.get(tripId));
    }

    // Backup periódico a Redis (recuperacon por reinicio)
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000) // cada 30 seg
    public void backupToRedis() {
        if (activeStates.isEmpty()) {
            return;
        }

        try {
            int count = 0;
            for (var entry : activeStates.entrySet()) {
                tripStateCacheDao.save(KEY_PREFIX + entry.getKey(), entry.getValue(), TTL_SECONDS);
                count++;
            }
            log.debug("TripStateManager: backup de {} estados a Redis", count);
        } catch (Exception e) {
            log.error("Error en backup de TripState a Redis", e);
        }
    }

    // Restaurar estados desde Redis al inicio (llamado manualmente o desde un
    // @PostConstruct)
    public void restoreFromRedis(List<Long> activeTripIds) {
        int restored = 0;
        for (Long tripId : activeTripIds) {
            Optional<TripState> stateOpt = tripStateCacheDao.get(KEY_PREFIX + tripId, TripState.class);
            if (stateOpt.isPresent()) {
                TripState state = stateOpt.get();
                activeStates.put(tripId, state);
                if (state.getVehicleId() != null) {
                    vehicleIdToTripId.put(state.getVehicleId(), tripId);
                }
                restored++;
            }
        }
        log.info("TripStateManager: restaurados {} estados desde Redis de {} trips activos", restored,
                activeTripIds.size());
    }

    // Total de estados activos (para métricas)
    public int getActiveCount() {
        return activeStates.size();
    }
}
