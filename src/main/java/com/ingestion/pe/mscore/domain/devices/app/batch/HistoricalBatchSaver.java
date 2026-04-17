package com.ingestion.pe.mscore.domain.devices.app.batch;

import com.ingestion.pe.mscore.domain.devices.core.entity.HistoricalDeviceEntity;
import com.ingestion.pe.mscore.domain.devices.core.repo.DeviceEntityRepository;
import com.ingestion.pe.mscore.domain.devices.core.repo.HistoricalDeviceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricalBatchSaver {

    // evitar OutOfMemory
    private final LinkedBlockingQueue<HistoricalDeviceEntity> queue = new LinkedBlockingQueue<>(20_000);
    
    private final HistoricalDeviceEntityRepository historicalRepo;
    private final DeviceEntityRepository deviceEntityRepository;

    /**
     * @param entity La entidad histórica a persistir.
     * @return true si fue aceptada, false si hubo timeout
     */
    public boolean enqueue(HistoricalDeviceEntity entity) {
        try {
            boolean accepted = queue.offer(entity, 10, TimeUnit.MILLISECONDS);
            if (!accepted) {
                log.warn("HistoricalBatchSaver: Cola llena ({}), aplicando backpressure", queue.size());
            } else {
            log.debug("HistoricalBatchSaver: Entidad encolada. Cola actual: {}", queue.size());
            }
            return accepted;
        } catch (InterruptedException e) {
            log.error("HistoricalBatchSaver: Interrupción al encolar", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Scheduled(fixedDelayString = "${historical.batch.fixed-delay:2000}", initialDelayString = "${historical.batch.initial-delay:5000}")
    public void flushBatch() {
        if (queue.isEmpty()) return;

        log.debug("HistoricalBatchSaver: Intentando vaciar cola. Tamaño actual: {}", queue.size());

        List<HistoricalDeviceEntity> batch = new ArrayList<>(1000);
        queue.drainTo(batch, 1000);

        if (batch.isEmpty()) return;

        try {
            long start = System.currentTimeMillis();
            
            batch.sort(Comparator.comparing(HistoricalDeviceEntity::getDeviceTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            historicalRepo.saveAllAndFlush(batch);
            
            long duration = System.currentTimeMillis() - start;
            log.info("HistoricalBatchSaver: Guardado exitoso de {} registros en {}ms. Primer ID: {}", 
                    batch.size(), duration, batch.get(0).getId());

            updateLatestHistoricalReferences(batch);

        } catch (Exception e) {
            log.error("HistoricalBatchSaver: Error crítico al persistir lote: {}", e.getMessage());
            
            int reinserted = 0;
            for (HistoricalDeviceEntity entity : batch) {
                if (queue.offer(entity)) reinserted++;
            }
            log.warn("HistoricalBatchSaver: {} registros re-encolados tras fallo de base de datos", reinserted);
        }
    }

    private void updateLatestHistoricalReferences(List<HistoricalDeviceEntity> batch) {
        Map<Long, HistoricalDeviceEntity> latestByDevice = new HashMap<>();

        for (HistoricalDeviceEntity h : batch) {
            Long dId = h.getDeviceId();
            latestByDevice.merge(dId, h, (existing, candidate) ->
                    (candidate.getDeviceTime() != null &&
                            (existing.getDeviceTime() == null || candidate.getDeviceTime().isAfter(existing.getDeviceTime())))
                            ? candidate : existing);
        }

        latestByDevice.forEach((deviceId, latest) -> {
            try {
                if (latest.getId() != null && deviceId != 0L) {
                    deviceEntityRepository.updateLastHistoricalDevice(deviceId, latest.getId());
                }
            } catch (Exception e) {
                log.warn("HistoricalBatchSaver: No se pudo actualizar referencia histórica para deviceId={}: {}",
                        deviceId, e.getMessage());
            }
        });
    }

    public int getPendingCount() {
        return queue.size();
    }
}
