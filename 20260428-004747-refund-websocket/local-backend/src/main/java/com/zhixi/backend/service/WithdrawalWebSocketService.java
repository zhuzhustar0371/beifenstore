package com.zhixi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WithdrawalWebSocketService extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(WithdrawalWebSocketService.class);
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    sessions.put(session.getId(), session);
    log.info("WebSocket connected: {}", session.getId());
    send(session, "connected", "ok");
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessions.remove(session.getId());
    log.info("WebSocket disconnected: {}", session.getId());
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    sessions.remove(session.getId());
    log.warn("WebSocket transport error: {}", session.getId(), exception);
  }

  public void broadcast(String event, Object payload) {
    TextMessage message = buildMessage(event, payload);
    for (WebSocketSession session : sessions.values()) {
      if (session.isOpen()) {
        try {
          synchronized (session) {
            session.sendMessage(message);
          }
        } catch (IOException e) {
          log.warn("Failed to send WebSocket message to {}", session.getId());
          sessions.remove(session.getId());
          try { session.close(); } catch (IOException ignored) {}
        }
      }
    }
  }

  private void send(WebSocketSession session, String event, Object payload) {
    try {
      session.sendMessage(buildMessage(event, payload));
    } catch (IOException e) {
      log.warn("Failed to send WebSocket message to {}", session.getId());
    }
  }

  private TextMessage buildMessage(String event, Object payload) {
    try {
      Map<String, Object> msg = new java.util.LinkedHashMap<>();
      msg.put("event", event);
      msg.put("data", payload);
      return new TextMessage(objectMapper.writeValueAsString(msg));
    } catch (IOException e) {
      return new TextMessage("{\"event\":\"" + event + "\",\"data\":null}");
    }
  }
}
