package com.zhixi.backend.service;

import com.zhixi.backend.model.WithdrawalRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WithdrawalEventService {
  private static final long STREAM_TIMEOUT_MS = 30L * 60L * 1000L;

  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
  private final WithdrawalWebSocketService withdrawalWebSocketService;

  public WithdrawalEventService(WithdrawalWebSocketService withdrawalWebSocketService) {
    this.withdrawalWebSocketService = withdrawalWebSocketService;
  }

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(error -> emitters.remove(emitter));
    try {
      emitter.send(SseEmitter.event().name("connected").data("ok"));
    } catch (IOException ex) {
      emitters.remove(emitter);
      emitter.completeWithError(ex);
    }
    return emitter;
  }

  public void publishCreated(WithdrawalRequest request) {
    publish("withdrawal-created", request);
    // Aggregated WebSocket broadcast
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("id", request.getId());
    summary.put("userId", request.getUserId());
    summary.put("amount", request.getAmount());
    summary.put("status", request.getStatus());
    summary.put("createdAt", request.getCreatedAt());
    withdrawalWebSocketService.enqueueWithdrawalEvent(summary);
  }

  private void publish(String eventName, Object payload) {
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(payload));
      } catch (IOException ex) {
        emitters.remove(emitter);
        emitter.completeWithError(ex);
      }
    }
  }
}
