package com.ingestion.pe.mscore.domain.atu.app.port;

import com.ingestion.pe.mscore.domain.atu.core.model.AtuPayload;

public interface AtuWebSocketPort {

    /**
     * @param token    Token de autenticación ATU de la empresa
     * @param endpoint URL WS completa (sin el parámetro token)
     * @param payload  Trama GPS a transmitir
     */
    boolean sendPayload(String token, String endpoint, AtuPayload payload);
}
