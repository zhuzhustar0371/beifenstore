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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WithdrawalWebSocketService extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(WithdrawalWebSocketService.class);
  private static final long AGGREGATION_WINDOW_MS = 5000;

  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  // Aggregation buffer
  private final List<Map<String, Object>> withdrawalBuffer = new ArrayList<>();
  private final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "ws-flush");
    t.setDaemon(true);
    return t;
  });
  private volatile boolean flushScheduled = false;

  public WithdrawalWebSocketService() {
    flushScheduler.scheduleAtFixedRate(this::flushWithdrawalBuffer,
        AGGREGATION_WINDOW_MS, AGGREGATION_WINDOW_MS, TimeUnit.MILLISECONDS);
  }

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

  /** Enqueue a withdrawal event for aggregated broadcast. */
  public synchronized void enqueueWithdrawalEvent(Map<String, Object> payload) {
    withdrawalBuffer.add(payload);
    flushScheduled = true;
  }

  /** Broadcast immediately without aggregation (used for non-withdrawal events). */
  public void broadcast(String event, Object payload) {
    TextMessage message = buildMessage(event, payload);
    broadcastRaw(message);
  }

  /** Push cashback status update to admin clients. */
  public void pushCashbackStatusUpdate(Long cashbackId, String status, Long userId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("cashbackId", cashbackId);
    data.put("status", status);
    data.put("userId", userId);
    broadcast("cashback-status-changed", data);
  }

  private synchronized void flushWithdrawalBuffer() {
    if (withdrawalBuffer.isEmpty()) {
      return;
    }
    List<Map<String, Object>> batch = new ArrayList<>(withdrawalBuffer);
    withdrawalBuffer.clear();
    flushScheduled = false;

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("count", batch.size());
    data.put("items", batch);
    TextMessage message = buildMessage("withdrawal-batch", data);
    broadcastRaw(message);
  }

  private void broadcastRaw(TextMessage message) {
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
      Map<String, Object> msg = new LinkedHashMap<>();
      msg.put("event", event);
      msg.put("data", payload);
      msg.put("timestamp", System.currentTimeMillis());
      return new TextMessage(objectMapper.writeValueAsString(msg));
    } catch (IOException e) {
      return new TextMessage("{\"event\":\"" + event + "\",\"data\":null}");
    }
  }
}
