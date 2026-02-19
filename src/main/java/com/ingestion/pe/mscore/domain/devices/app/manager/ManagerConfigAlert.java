package com.ingestion.pe.mscore.domain.devices.app.manager;

import com.ingestion.pe.mscore.domain.devices.app.cache.CacheRepository;
import com.ingestion.pe.mscore.domain.devices.core.entity.DeviceConfigAlertsEntity;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ManagerConfigAlert {

    // Repositorio que mantiene el estado runtime de las alertas
    // (contador de true/false, activo/inactivo, etc.)
    // Este estado NO es persistencia de negocio, es estado operativo
    private final AlertRuntimeStateRepository stateRepo;

    /**
     * Evalúa las reglas de configuración de alertas para un dispositivo y decide si
     * una alerta debe
     * activarse o desactivarse.
     *
     * @param deviceId   ID del dispositivo
     * @param attributes atributos actuales del dispositivo (sensores, estados,
     *                   etc.)
     * @return conjunto de relaciones DeviceConfigAlertsEntity que cambiaron de
     *         estado
     */
    public Set<DeviceConfigAlertsEntity> executeConfigAlertRules(
            Long deviceId, Map<String, Object> attributes) {

        // Respuesta con las alertas que sufrieron un cambio de estado
        Set<DeviceConfigAlertsEntity> response = new HashSet<>();

        // Obtiene todas las configuraciones de alertas asociadas al dispositivo desde
        // cache
        var configsAlerts = CacheRepository.get(deviceId);

        // Itera cada relación dispositivo <-> configuración de alerta
        for (DeviceConfigAlertsEntity relation : configsAlerts) {
            try {
                // Obtiene la configuración de la alerta (reglas, thresholds, etc.)
                var config = relation.getConfigAlerts();

                // Evalúa la regla JEXL con los atributos actuales del dispositivo
                boolean result = config.evaluateJexlScript(attributes);

                // Obtiene o crea el estado runtime de esta alerta para este dispositivo
                // Aquí se mantiene el conteo de true/false consecutivos
                AlertRuntimeState state = stateRepo.getOrCreate(deviceId, config.getId());

                // Actualiza el estado runtime según el resultado de la evaluación
                if (result) {
                    // Incrementa contadorde true y reinicia contador de false si aplica
                    state.onTrue();
                } else {
                    // Incrementa contador de false y reinicia contador de true si aplica
                    state.onFalse();
                }

                // Threshold de activación:
                // cuántas veces consecutivas debe cumplirse la condición para activarse
                long activateThreshold = Optional.ofNullable(config.getCountEventForActivate()).orElse(1L);

                // Threshold de desactivación:
                // cuántas veces consecutivas debe fallar la condición para desactivarse
                long deactivateThreshold = Optional.ofNullable(config.getCountEventForDeactivate()).orElse(1L);

                // Si la alerta NO está activa y ya alcanzó el threshold de activación
                if (!state.isActive() && state.getTrueCount() >= activateThreshold) {

                    // Marca la relación como activa (transición INACTIVA -> ACTIVA)
                    relation.activate();

                    // Actualiza el estado runtime a activo y resetea contadores
                    state.activate();

                    // Se añade a la respuesta porque hubo un cambio de estado
                    response.add(relation);
                }

                // Si la alerta está activa y ya alcanzó el threshold de desactivación
                if (state.isActive() && state.getFalseCount() >= deactivateThreshold) {

                    // Marca la relación como inactiva (transición ACTIVA -> INACTIVA)
                    relation.markAsResolved();

                    // Actualiza el estado runtime a inactivo y resetea contadores
                    state.deactivate();

                    // Se añade a la respuesta porque hubo un cambio de estado
                    response.add(relation);
                }

                // Persiste el estado runtime actualizado
                stateRepo.save(deviceId, config.getId(), state);

            } catch (Exception e) {
                // Captura errores de evaluación para evitar romper el flujo completo
                log.error("Error al evaluar ConfigAlert ID {}: {}", relation.getId(), e.getMessage());
            }
        }

        // Devuelve únicamente las alertas que cambiaron de estado
        return response;
    }
}
