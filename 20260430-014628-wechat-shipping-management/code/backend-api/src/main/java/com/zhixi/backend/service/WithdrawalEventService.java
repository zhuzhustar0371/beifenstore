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
  private final UserWebSocketService userWebSocketService;

  public WithdrawalEventService(
      WithdrawalWebSocketService withdrawalWebSocketService,
      UserWebSocketService userWebSocketService
  ) {
    this.withdrawalWebSocketService = withdrawalWebSocketService;
    this.userWebSocketService = userWebSocketService;
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
    Map<String, Object> summary = toSummary(request);
    withdrawalWebSocketService.enqueueWithdrawalEvent(summary);
    userWebSocketService.pushToUser(request.getUserId(), "withdrawal-request-created", summary);
  }

  public void publishStatusChanged(WithdrawalRequest request) {
    if (request == null) {
      return;
    }
    Map<String, Object> summary = toSummary(request);
    publish("withdrawal-status-changed", request);
    withdrawalWebSocketService.broadcast("withdrawal-status-changed", summary);
    userWebSocketService.pushToUser(request.getUserId(), "withdrawal-request-status-changed", summary);
  }

  private Map<String, Object> toSummary(WithdrawalRequest request) {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("id", request.getId());
    summary.put("userId", request.getUserId());
    summary.put("amount", request.getAmount());
    summary.put("requestedAmount", request.getRequestedAmount());
    summary.put("suggestedAmount", request.getSuggestedAmount());
    summary.put("readyAmount", request.getReadyAmount());
    summary.put("pendingAmount", request.getPendingAmount());
    summary.put("status", request.getStatus());
    summary.put("applyMode", request.getApplyMode());
    summary.put("createdAt", request.getCreatedAt());
    return summary;
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
