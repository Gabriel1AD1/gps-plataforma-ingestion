package com.ingestion.pe.mscore.domain.atu.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingestion.pe.mscore.domain.atu.core.model.AtuPayload;
import com.ingestion.pe.mscore.domain.atu.core.model.AtuResponse;
import com.ingestion.pe.mscore.domain.atu.app.port.AtuWebSocketPort;
import java.net.URI;
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

    private final StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

    @Override
    public void sendPayload(String token, String endpoint, AtuPayload payload) {
        try {
            WebSocketSession session = getOrConnectSession(token, endpoint);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(jsonPayload));
            
            log.debug("Trama WS ATU enviada correctamente para IMEI={} con Token={}", payload.getImei(), truncateToken(token));
        } catch (Exception e) {
            log.error("Error enviando trama WS ATU para IMEI={}: {}", payload.getImei(), e.getMessage());
            sessionsByToken.remove(token);
        }
    }

    private WebSocketSession getOrConnectSession(String token, String endpoint) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        WebSocketSession existingSession = sessionsByToken.get(token);
        if (existingSession != null && existingSession.isOpen()) {
            return existingSession;
        }

        if (existingSession != null) {
            sessionsByToken.remove(token);
        }

        String wsUrl = endpoint;
        if (!wsUrl.contains("?token=")) {
            wsUrl += "?token=" + token;
        }

        log.info("Conectando al WebSocket ATU: {}", wsUrl);

        WebSocketSession newSession = webSocketClient.execute(new AtuWebSocketHandler(token), wsUrl)
                .get(10, TimeUnit.SECONDS);
                
        sessionsByToken.put(token, newSession);
        return newSession;
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
                if (response.isSuccess()) { // "00"
                    log.debug("Trama procesada correctamente por ATU (Identificador={})", response.getIdentifier());
                } else {
                    log.warn("ATU rechazó la trama. Código: {} - {}. Identifier: {}", 
                            response.getCode(), response.getMessage(), response.getIdentifier());
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
