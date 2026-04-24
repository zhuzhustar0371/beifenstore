package com.zhixi.backend.controller;

import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import com.zhixi.backend.model.Order;
import com.zhixi.backend.service.OrderService;
import com.zhixi.backend.service.WechatPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay/wechat")
public class PayNotifyController {
  private static final Logger log = LoggerFactory.getLogger(PayNotifyController.class);

  private final WechatPayService wechatPayService;
  private final OrderService orderService;

  public PayNotifyController(WechatPayService wechatPayService, OrderService orderService) {
    this.wechatPayService = wechatPayService;
    this.orderService = orderService;
  }

  @PostMapping("/notify")
  public ResponseEntity<Map<String, String>> handleNotify(HttpServletRequest request) {
    try {
      RequestParam requestParam = buildRequestParam(request);
      NotificationParser parser = new NotificationParser((NotificationConfig) wechatPayService.getConfig());
      Transaction transaction = parser.parse(requestParam, Transaction.class);

      log.info(
          "Received WeChat Pay callback, outTradeNo={}, transactionId={}, tradeState={}, payerOpenidPresent={}",
          transaction.getOutTradeNo(),
          transaction.getTransactionId(),
          transaction.getTradeState(),
          hasText(resolvePayerOpenid(transaction))
      );

      if (Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
        String payerOpenid = resolvePayerOpenid(transaction);
        Order order = orderService.getOrderByOrderNo(transaction.getOutTradeNo());
        if ("PENDING".equals(order.getStatus())) {
          LocalDateTime paidAt = resolvePaidAt(transaction.getSuccessTime());
          orderService.markPaid(order.getId(), paidAt, "WECHAT", transaction.getTransactionId(), payerOpenid);
        } else {
          orderService.recordPaymentOpenid(order.getUserId(), payerOpenid);
          log.info("Skip duplicated callback for orderNo={}, status={}", order.getOrderNo(), order.getStatus());
        }
      }

      return ResponseEntity.ok(successBody());
    } catch (Exception e) {
      log.error("Failed to process WeChat Pay callback", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failBody());
    }
  }

  @PostMapping("/refund/notify")
  public ResponseEntity<Map<String, String>> handleRefundNotify(HttpServletRequest request) {
    try {
      RequestParam requestParam = buildRequestParam(request);
      NotificationParser parser = new NotificationParser((NotificationConfig) wechatPayService.getConfig());
      RefundNotification refund = parser.parse(requestParam, RefundNotification.class);

      log.info(
          "Received WeChat refund callback, outRefundNo={}, refundId={}, refundStatus={}",
          refund.getOutRefundNo(),
          refund.getRefundId(),
          refund.getRefundStatus()
      );

      if (Status.SUCCESS.equals(refund.getRefundStatus())) {
        orderService.markRefundSuccessByRefundNo(refund.getOutRefundNo(), refund.getRefundId());
      } else if (Status.CLOSED.equals(refund.getRefundStatus()) || Status.ABNORMAL.equals(refund.getRefundStatus())) {
        orderService.markRefundFailedByRefundNo(refund.getOutRefundNo(), refund.getRefundId());
      }

      return ResponseEntity.ok(successBody());
    } catch (Exception e) {
      log.error("Failed to process WeChat refund callback", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failBody());
    }
  }

  private RequestParam buildRequestParam(HttpServletRequest request) throws java.io.IOException {
    String body = readBody(request);
    return new RequestParam.Builder()
        .serialNumber(request.getHeader("Wechatpay-Serial"))
        .nonce(request.getHeader("Wechatpay-Nonce"))
        .signature(request.getHeader("Wechatpay-Signature"))
        .timestamp(request.getHeader("Wechatpay-Timestamp"))
        .body(body)
        .build();
  }

  private String readBody(HttpServletRequest request) throws java.io.IOException {
    BufferedReader reader = request.getReader();
    StringBuilder builder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
    }
    return builder.toString();
  }

  private LocalDateTime resolvePaidAt(String successTime) {
    if (successTime == null || successTime.isBlank()) {
      return LocalDateTime.now();
    }
    return OffsetDateTime.parse(successTime).toLocalDateTime();
  }

  private String resolvePayerOpenid(Transaction transaction) {
    if (transaction == null || transaction.getPayer() == null) {
      return null;
    }
    return transaction.getPayer().getOpenid();
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private Map<String, String> successBody() {
    Map<String, String> response = new HashMap<>();
    response.put("code", "SUCCESS");
    response.put("message", "success");
    return response;
  }

  private Map<String, String> failBody() {
    Map<String, String> error = new HashMap<>();
    error.put("code", "FAIL");
    error.put("message", "failed");
    return error;
  }
}
