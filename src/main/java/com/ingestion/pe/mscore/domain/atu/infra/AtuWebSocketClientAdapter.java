package com.ingestion.pe.mscore.domain.atu.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingestion.pe.mscore.domain.atu.core.model.AtuPayload;
import com.ingestion.pe.mscore.domain.atu.core.model.AtuResponse;
import com.ingestion.pe.mscore.domain.atu.app.port.AtuWebSocketPort;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class AtuWebSocketClientAdapter implements AtuWebSocketPort {

    private final ObjectMapper objectMapper;
    
    private final ConcurrentHashMap<String, WebSocketSession> sessionsByToken = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> nextRetryByToken = new ConcurrentHashMap<>();

    private final StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

    @Override
    public boolean sendPayload(String token, String endpoint, AtuPayload payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.info("[ATU JSON] {}", jsonPayload);

            WebSocketSession session = getOrConnectSession(token, endpoint);
            
            session.sendMessage(new TextMessage(jsonPayload));
            
            log.info("enviocorrectamenteatu");
            log.info("[ATU] Trama WS enviada correctamente para IMEI={} a {}", payload.getImei(), endpoint);
            return true;
        } catch (Exception e) {
            log.error("enviofallidoatu");
            log.error("Error enviando trama WS ATU para IMEI={}: {}", payload.getImei(), e.getMessage());
            
            sessionsByToken.remove(token);
            return false;
        }
    }

    private WebSocketSession getOrConnectSession(String token, String endpoint) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        WebSocketSession existingSession = sessionsByToken.get(token);
        if (existingSession != null && existingSession.isOpen()) {
            return existingSession;
        }

        Instant nextRetry = nextRetryByToken.get(token);
        if (nextRetry != null && Instant.now().isBefore(nextRetry)) {
            throw new RuntimeException("ATU Connection Cooldown: Esperando hasta " + nextRetry + " para reintentar conexión.");
        }

        if (existingSession != null) {
            sessionsByToken.remove(token);
        }

        String wsUrl = endpoint;
        if (wsUrl.contains("token=")) {
            if (wsUrl.endsWith("token=")) {
                wsUrl += token;
            }
        } else {
            wsUrl += (wsUrl.contains("?") ? "&" : "?") + "token=" + token;
        }

        log.info("Conectando al WebSocket ATU: {}", wsUrl);

        try {
            WebSocketSession newSession = webSocketClient.execute(new AtuWebSocketHandler(token), wsUrl)
                    .get(10, TimeUnit.SECONDS);
            
            nextRetryByToken.remove(token);
            sessionsByToken.put(token, newSession);
            
            Thread.sleep(300);
            
            return newSession;
        } catch (Exception e) {
            nextRetryByToken.put(token, Instant.now().plusSeconds(30));
            log.error("Fallo al establecer conexión con ATU. Cooldown activado (30s). Error: {}", e.getMessage());
            throw e;
        }
    }

    private String truncateToken(String token) {
        if (token == null) return "null";
        if (token.length() <= 8) return token;
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private class AtuWebSocketHandler extends TextWebSocketHandler {
        private final String token;

        public AtuWebSocketHandler(String token) {
            this.token = token;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("Conexión WS ATU establecida exitosamente (SessionID={})", session.getId());
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            try {
                String payload = message.getPayload();
                log.debug("Respuesta WS ATU cruda: {}", payload);
                
                AtuResponse response = objectMapper.readValue(payload, AtuResponse.class);
                if (response == null) {
                    log.error("[ATU] Error crítico: El mapeo de la respuesta ATU retornó null.");
                    return;
                }

                if (response.isSuccess()) {
                    log.info("[ATU] Trama procesada correctamente por servidor ATU (Identificador={})", response.getIdentifier());
                } else {
                    log.warn("[ATU] Servidor rechazó la trama. Código: {} - {}. Identifier: {} - Respuesta completa: {}", 
                            response.getCode(), response.getMessage(), response.getIdentifier(), payload);
                }
            } catch (Exception e) {
                log.error("Error parseando respuesta WS de la ATU: {}", e.getMessage());
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.warn("Conexión WS ATU cerrada (SessionID={}, Status={}). Limpiando sesión del pool.", session.getId(), status);
            sessionsByToken.remove(this.token);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("Error de transporte en WS ATU (SessionID={}): {}", session.getId(), exception.getMessage());
            sessionsByToken.remove(this.token);
        }
    }
}
