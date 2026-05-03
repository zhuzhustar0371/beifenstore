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
public class UserWebSocketService extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(UserWebSocketService.class);
  private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId == null) {
      try { session.close(); } catch (IOException ignored) {}
      return;
    }
    sessions.put(userId, session);
    log.info("User WebSocket connected: userId={}, session={}", userId, session.getId());
    send(session, "connected", "ok");
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId != null) {
      sessions.remove(userId);
      log.info("User WebSocket disconnected: userId={}", userId);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId != null) {
      sessions.remove(userId);
    }
  }

  public void pushToUser(Long userId, String event, Object payload) {
    WebSocketSession session = sessions.get(userId);
    if (session != null && session.isOpen()) {
      try {
        synchronized (session) {
          session.sendMessage(buildMessage(event, payload));
        }
      } catch (IOException e) {
        log.warn("Failed to push to userId={}", userId);
        sessions.remove(userId);
      }
    }
  }

  public boolean isUserConnected(Long userId) {
    WebSocketSession session = sessions.get(userId);
    return session != null && session.isOpen();
  }

  private void send(WebSocketSession session, String event, Object payload) {
    try {
      session.sendMessage(buildMessage(event, payload));
    } catch (IOException e) {
      log.warn("Failed to send to session {}", session.getId());
    }
  }

  private TextMessage buildMessage(String event, Object payload) {
    try {
      Map<String, Object> msg = new java.util.LinkedHashMap<>();
      msg.put("event", event);
      msg.put("data", payload);
      msg.put("timestamp", System.currentTimeMillis());
      return new TextMessage(objectMapper.writeValueAsString(msg));
    } catch (IOException e) {
      return new TextMessage("{\"event\":\"" + event + "\",\"data\":null}");
    }
  }
}
