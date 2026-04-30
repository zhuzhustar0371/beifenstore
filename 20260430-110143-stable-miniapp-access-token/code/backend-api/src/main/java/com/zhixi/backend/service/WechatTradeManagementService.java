package com.zhixi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.mapper.OrderMapper;
import com.zhixi.backend.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class WechatTradeManagementService {
  private static final Logger log = LoggerFactory.getLogger(WechatTradeManagementService.class);
  private static final int ORDER_STATE_WAIT_SHIP = 1;
  private static final int ORDER_STATE_SHIPPED = 2;
  private static final int ORDER_STATE_CONFIRMED = 3;
  private static final int ORDER_STATE_COMPLETED = 4;
  private static final int ORDER_STATE_REFUNDED = 5;
  private static final int ORDER_STATE_SETTLED = 6;
  private static final int ERR_ALREADY_SHIPPED = 10060003;
  private static final int ERR_SHIPPING_UNCHANGED = 10060023;
  private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

  private final ObjectMapper objectMapper;
  private final OrderMapper orderMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Value("${app.wechat.miniapp.app-id:}")
  private String miniappAppId;

  @Value("${app.wechat.miniapp.app-secret:}")
  private String miniappAppSecret;

  @Value("${app.wechat.pay.mchid:}")
  private String payMchid;

  @Value("${app.wechat.trade-management.msg-jump-path:pages/order-list/order-list?tab=unreceive}")
  private String tradeManagementMsgJumpPath;

  private volatile String accessToken;
  private volatile LocalDateTime accessTokenExpiresAt;
  private volatile boolean messageJumpPathConfigured;

  public WechatTradeManagementService(ObjectMapper objectMapper, OrderMapper orderMapper) {
    this.objectMapper = objectMapper;
    this.orderMapper = orderMapper;
  }

  public void uploadShippingInfo(Order order, String payerOpenid, String trackingNo, String expressCompany, String itemDesc) {
    validateManagedOrder(order);
    if (isBlank(payerOpenid)) {
      throw new BusinessException("支付用户 openid 缺失，无法向微信平台录入发货信息");
    }

    ensureMessageJumpPathConfigured();

    TradeOrderLookupResult lookupResult = queryTradeOrderWithFallback(order);
    if (lookupResult.snapshot().orderState() >= ORDER_STATE_SHIPPED) {
      log.info(
          "Skip WeChat shipping upload because trade order is already shipped, orderId={}, orderNo={}, lookup={}, orderState={}",
          order.getId(),
          order.getOrderNo(),
          lookupResult.lookup().description(),
          lookupResult.snapshot().orderState()
      );
      return;
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("order_key", lookupResult.lookup().orderKey());
    payload.put("logistics_type", 1);
    payload.put("delivery_mode", 1);
    payload.put("shipping_list", buildShippingList(trackingNo, expressCompany, itemDesc));
    payload.put("upload_time", OffsetDateTime.now(SHANGHAI_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    payload.put("payer", Map.of("openid", payerOpenid.trim()));

    try {
      postWechatJson(
          "/wxa/sec/order/upload_shipping_info",
          payload,
          "微信发货信息录入失败"
      );
    } catch (WechatTradeManagementException ex) {
      if (ex.getErrCode() == ERR_ALREADY_SHIPPED || ex.getErrCode() == ERR_SHIPPING_UNCHANGED) {
        TradeOrderLookupResult latestLookup = queryTradeOrderWithFallback(order);
        if (latestLookup.snapshot().orderState() >= ORDER_STATE_SHIPPED) {
          log.info(
              "Treat WeChat shipping upload as idempotent success, orderId={}, transactionId={}, lookup={}, orderState={}",
              order.getId(),
              order.getTransactionId(),
              latestLookup.lookup().description(),
              latestLookup.snapshot().orderState()
          );
          return;
        }
      }
      if (isPaymentMissing(ex)) {
        throw new BusinessException(buildPaymentMissingMessage(order, "发货信息录入", buildOrderLookups(order), ex.getMessage()));
      }
      throw new BusinessException(ex.getMessage());
    }
  }

  public TradeManagementSyncResult syncOrderStatus(Order order) {
    validateManagedOrder(order);
    TradeOrderSnapshot snapshot = queryTradeOrderWithFallback(order).snapshot();

    if (!"REFUNDED".equals(order.getStatus()) && shouldMarkCompleted(snapshot.orderState())) {
      LocalDateTime completedAt = snapshot.resolveCompletedAt();
      orderMapper.markCompleted(order.getId(), completedAt == null ? LocalDateTime.now() : completedAt);
    }

    Order latest = orderMapper.findById(order.getId());
    return new TradeManagementSyncResult(
        latest,
        snapshot.orderState(),
        describeOrderState(snapshot.orderState())
    );
  }

  private TradeOrderLookupResult queryTradeOrderWithFallback(Order order) {
    List<TradeOrderLookup> lookups = buildOrderLookups(order);
    WechatTradeManagementException lastPaymentMissing = null;

    for (TradeOrderLookup lookup : lookups) {
      try {
        Map<String, Object> response = postWechatJson(
            "/wxa/sec/order/get_order",
            lookup.queryPayload(),
            "微信订单发货状态查询失败"
        );
        TradeOrderSnapshot snapshot = parseTradeOrderSnapshot(response);
        log.info(
            "Matched WeChat trade order, orderId={}, orderNo={}, lookup={}, orderState={}",
            order.getId(),
            order.getOrderNo(),
            lookup.description(),
            snapshot.orderState()
        );
        return new TradeOrderLookupResult(lookup, snapshot);
      } catch (WechatTradeManagementException ex) {
        if (!isPaymentMissing(ex)) {
          throw new BusinessException(ex.getMessage());
        }
        lastPaymentMissing = ex;
        log.warn(
            "WeChat trade order lookup missed, orderId={}, orderNo={}, lookup={}, message={}",
            order.getId(),
            order.getOrderNo(),
            lookup.description(),
            ex.getMessage()
        );
      }
    }

    String detail = lastPaymentMissing == null ? "" : lastPaymentMissing.getMessage();
    throw new BusinessException(buildPaymentMissingMessage(order, "订单发货状态查询", lookups, detail));
  }

  private TradeOrderSnapshot parseTradeOrderSnapshot(Map<String, Object> response) {
    Map<String, Object> orderPayload = mapValue(response.get("order"));
    if (orderPayload.isEmpty()) {
      throw new BusinessException("微信订单发货状态查询失败：响应缺少订单信息");
    }

    return new TradeOrderSnapshot(
        numberValue(orderPayload.get("order_state")),
        epochSecondToLocalDateTime(orderPayload.get("confirm_receive_time")),
        epochSecondToLocalDateTime(orderPayload.get("settlement_time")),
        epochSecondToLocalDateTime(orderPayload.get("pay_time"))
    );
  }

  private List<TradeOrderLookup> buildOrderLookups(Order order) {
    List<TradeOrderLookup> lookups = new ArrayList<>();
    if (!isBlank(order.getTransactionId())) {
      Map<String, Object> queryPayload = new LinkedHashMap<>();
      queryPayload.put("transaction_id", order.getTransactionId().trim());

      Map<String, Object> orderKey = new LinkedHashMap<>();
      orderKey.put("order_number_type", 2);
      orderKey.put("transaction_id", order.getTransactionId().trim());
      lookups.add(new TradeOrderLookup(
          queryPayload,
          orderKey,
          "transaction_id=" + maskId(order.getTransactionId()),
          "transaction_id=" + normalizeText(order.getTransactionId())
      ));
    }

    if (!isBlank(order.getOrderNo()) && !isBlank(payMchid)) {
      String mchid = payMchid.trim();
      Map<String, Object> queryPayload = new LinkedHashMap<>();
      queryPayload.put("merchant_id", mchid);
      queryPayload.put("merchant_trade_no", normalizeText(order.getOrderNo()));

      Map<String, Object> orderKey = new LinkedHashMap<>();
      orderKey.put("order_number_type", 1);
      orderKey.put("mchid", mchid);
      orderKey.put("out_trade_no", normalizeText(order.getOrderNo()));
      lookups.add(new TradeOrderLookup(
          queryPayload,
          orderKey,
          "mchid=" + maskId(mchid) + ", out_trade_no=" + normalizeText(order.getOrderNo()),
          "mchid=" + mchid + " + out_trade_no=" + normalizeText(order.getOrderNo())
      ));
    }

    if (lookups.isEmpty()) {
      if (!isBlank(order.getOrderNo())) {
        requirePayMchid();
      }
      throw new BusinessException("订单缺少微信支付单号和商户订单号，无法调用发货管理接口");
    }
    return lookups;
  }

  private List<Map<String, Object>> buildShippingList(String trackingNo, String expressCompany, String itemDesc) {
    String normalizedTrackingNo = normalizeText(trackingNo);
    String normalizedExpressCompany = normalizeText(expressCompany).toUpperCase(Locale.ROOT);
    String normalizedItemDesc = normalizeText(itemDesc);

    if (isBlank(normalizedTrackingNo)) {
      throw new BusinessException("物流单号不能为空");
    }
    if (isBlank(normalizedExpressCompany)) {
      throw new BusinessException("物流公司编码不能为空");
    }
    if (isBlank(normalizedItemDesc)) {
      throw new BusinessException("商品描述不能为空");
    }

    Map<String, Object> shipping = new LinkedHashMap<>();
    shipping.put("tracking_no", normalizedTrackingNo);
    shipping.put("express_company", normalizedExpressCompany);
    shipping.put("item_desc", normalizedItemDesc);

    List<Map<String, Object>> shippingList = new ArrayList<>();
    shippingList.add(shipping);
    return shippingList;
  }

  private void ensureMessageJumpPathConfigured() {
    if (isBlank(tradeManagementMsgJumpPath) || messageJumpPathConfigured) {
      return;
    }

    synchronized (this) {
      if (messageJumpPathConfigured) {
        return;
      }
      String path = tradeManagementMsgJumpPath.trim();
      try {
        postWechatJson(
            "/wxa/sec/order/set_msg_jump_path",
            Map.of("path", path),
            "微信发货消息跳转路径设置失败"
        );
        messageJumpPathConfigured = true;
        log.info("Configured WeChat trade management message jump path, path={}", path);
      } catch (WechatTradeManagementException ex) {
        log.warn("Skip WeChat trade management message jump path configuration, path={}, message={}", path, ex.getMessage());
      }
    }
  }

  private Map<String, Object> postWechatJson(String path, Map<String, Object> payload, String fallbackMessage) {
    try {
      String url = "https://api.weixin.qq.com"
          + path
          + "?access_token="
          + URLEncoder.encode(getAccessToken(), StandardCharsets.UTF_8);
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new WechatTradeManagementException(-1, fallbackMessage + "：HTTP " + response.statusCode());
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
      int errCode = numberValue(body.get("errcode"));
      if (errCode != 0) {
        String errMsg = stringValue(body.get("errmsg"));
        log.warn("WeChat trade management API returned error, path={}, errcode={}, errmsg={}", path, errCode, errMsg);
        throw new WechatTradeManagementException(errCode, fallbackMessage + "：" + errMsg);
      }
      return body;
    } catch (WechatTradeManagementException ex) {
      throw ex;
    } catch (BusinessException ex) {
      throw new WechatTradeManagementException(-1, ex.getMessage());
    } catch (Exception ex) {
      throw new WechatTradeManagementException(-1, fallbackMessage);
    }
  }

  private String getAccessToken() {
    validateMiniappConfig();
    if (hasValidAccessToken()) {
      return accessToken;
    }

    synchronized (this) {
      if (hasValidAccessToken()) {
        return accessToken;
      }

      try {
        String url = "https://api.weixin.qq.com/cgi-bin/token"
            + "?grant_type=client_credential"
            + "&appid=" + URLEncoder.encode(miniappAppId, StandardCharsets.UTF_8)
            + "&secret=" + URLEncoder.encode(miniappAppSecret, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          throw new BusinessException("获取微信 access_token 失败：HTTP " + response.statusCode());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(response.body(), Map.class);
        int errCode = numberValue(payload.get("errcode"));
        if (errCode != 0) {
          throw new BusinessException("获取微信 access_token 失败：" + stringValue(payload.get("errmsg")));
        }

        String nextAccessToken = stringValue(payload.get("access_token"));
        if (nextAccessToken.isBlank()) {
          throw new BusinessException("获取微信 access_token 失败：响应为空");
        }

        long expiresIn = numberValue(payload.get("expires_in"));
        if (expiresIn <= 0) {
          expiresIn = 7200L;
        }
        accessToken = nextAccessToken;
        accessTokenExpiresAt = LocalDateTime.now().plusSeconds(Math.max(expiresIn - 300L, 60L));
        return accessToken;
      } catch (BusinessException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BusinessException("获取微信 access_token 失败");
      }
    }
  }

  private boolean hasValidAccessToken() {
    return !isBlank(accessToken)
        && accessTokenExpiresAt != null
        && accessTokenExpiresAt.isAfter(LocalDateTime.now());
  }

  private boolean shouldMarkCompleted(int orderState) {
    return orderState == ORDER_STATE_CONFIRMED
        || orderState == ORDER_STATE_COMPLETED
        || orderState == ORDER_STATE_SETTLED;
  }

  private String describeOrderState(int orderState) {
    return switch (orderState) {
      case ORDER_STATE_WAIT_SHIP -> "WAIT_SHIP";
      case ORDER_STATE_SHIPPED -> "SHIPPED";
      case ORDER_STATE_CONFIRMED -> "CONFIRMED";
      case ORDER_STATE_COMPLETED -> "COMPLETED";
      case ORDER_STATE_REFUNDED -> "REFUNDED";
      case ORDER_STATE_SETTLED -> "SETTLED";
      default -> "UNKNOWN";
    };
  }

  private void validateManagedOrder(Order order) {
    if (order == null) {
      throw new BusinessException("订单不存在");
    }
    if (!"WECHAT".equals(order.getPayType()) && isBlank(order.getTransactionId())) {
      throw new BusinessException("当前订单不是微信支付订单");
    }
    if (isBlank(order.getOrderNo())) {
      throw new BusinessException("订单号缺失，无法调用微信发货管理接口");
    }
  }

  private void validateMiniappConfig() {
    if (isBlank(miniappAppId)) {
      throw new BusinessException("WECHAT_MINIAPP_APP_ID 未配置");
    }
    if (isBlank(miniappAppSecret)) {
      throw new BusinessException("WECHAT_MINIAPP_APP_SECRET 未配置");
    }
  }

  private String requirePayMchid() {
    if (isBlank(payMchid)) {
      throw new BusinessException("WECHAT_PAY_MCHID 未配置");
    }
    return payMchid.trim();
  }

  private Map<String, Object> mapValue(Object value) {
    if (value instanceof Map<?, ?> rawMap) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
        if (entry.getKey() != null) {
          result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
      }
      return result;
    }
    return Map.of();
  }

  private int numberValue(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }

  private LocalDateTime epochSecondToLocalDateTime(Object value) {
    if (!(value instanceof Number number)) {
      return null;
    }
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(number.longValue()), ZoneOffset.ofHours(8));
  }

  private String stringValue(Object value) {
    return value instanceof String text ? text.trim() : "";
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private boolean isPaymentMissing(WechatTradeManagementException ex) {
    if (ex == null || ex.getMessage() == null) {
      return false;
    }
    String message = ex.getMessage();
    String lower = message.toLowerCase(Locale.ROOT);
    return message.contains("支付单不存在")
        || message.contains("订单不存在")
        || lower.contains("order not exist")
        || lower.contains("payment not exist");
  }

  private String buildPaymentMissingMessage(Order order, String action, List<TradeOrderLookup> lookups, String detail) {
    StringJoiner tried = new StringJoiner("；");
    for (TradeOrderLookup lookup : lookups) {
      tried.add(lookup.adminText());
    }

    StringBuilder message = new StringBuilder();
    message.append("微信").append(action).append("失败：支付单不存在。已尝试 ")
        .append(tried)
        .append("。请核对该订单是否为当前小程序 AppID、当前商户号 MCHID 下真实支付成功的微信支付单，并确认本地 transaction_id 与微信支付订单号一致。")
        .append("本地 orderId=").append(order == null ? "-" : order.getId())
        .append(", orderNo=").append(order == null ? "-" : normalizeText(order.getOrderNo()))
        .append(", payType=").append(order == null ? "-" : normalizeText(order.getPayType()))
        .append(", transactionId=").append(order == null ? "-" : normalizeText(order.getTransactionId()));
    if (!isBlank(detail)) {
      message.append("。微信原始错误：").append(detail);
    }
    return message.toString();
  }

  private String maskId(String value) {
    String text = normalizeText(value);
    if (text.length() <= 8) {
      return text;
    }
    return text.substring(0, 4) + "..." + text.substring(text.length() - 4);
  }

  public record TradeManagementSyncResult(
      Order order,
      Integer tradeOrderState,
      String tradeOrderStateText
  ) {
  }

  private record TradeOrderLookup(
      Map<String, Object> queryPayload,
      Map<String, Object> orderKey,
      String description,
      String adminText
  ) {
  }

  private record TradeOrderLookupResult(
      TradeOrderLookup lookup,
      TradeOrderSnapshot snapshot
  ) {
  }

  private record TradeOrderSnapshot(
      int orderState,
      LocalDateTime confirmReceiveTime,
      LocalDateTime settlementTime,
      LocalDateTime payTime
  ) {
    private LocalDateTime resolveCompletedAt() {
      if (settlementTime != null) {
        return settlementTime;
      }
      if (confirmReceiveTime != null) {
        return confirmReceiveTime;
      }
      return payTime;
    }
  }

  private static final class WechatTradeManagementException extends RuntimeException {
    private final int errCode;

    private WechatTradeManagementException(int errCode, String message) {
      super(message);
      this.errCode = errCode;
    }

    private int getErrCode() {
      return errCode;
    }
  }
}
