package com.zhixi.backend.controller;

import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.CreateOrderRequest;
import com.zhixi.backend.dto.UserRefundRequestCreateRequest;
import com.zhixi.backend.model.Order;
import com.zhixi.backend.model.User;
import com.zhixi.backend.service.OrderService;
import com.zhixi.backend.service.UserAuthService;
import com.zhixi.backend.service.WechatTradeManagementService;
import com.zhixi.backend.service.WechatPayService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;
  private final WechatPayService wechatPayService;
  private final UserAuthService userAuthService;
  private final WechatTradeManagementService wechatTradeManagementService;

  @Value("${app.pay.mock-enabled:false}")
  private boolean mockPayEnabled;

  @Value("${app.wechat.pay.mchid:}")
  private String payMchid;

  public OrderController(
      OrderService orderService,
      WechatPayService wechatPayService,
      UserAuthService userAuthService,
      WechatTradeManagementService wechatTradeManagementService
  ) {
    this.orderService = orderService;
    this.wechatPayService = wechatPayService;
    this.userAuthService = userAuthService;
    this.wechatTradeManagementService = wechatTradeManagementService;
  }

  @PostMapping
  public ApiResponse<Order> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody CreateOrderRequest request
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(withMerchantId(orderService.createOrder(currentUser.getId(), request)));
  }

  @PostMapping("/{orderId}/pay")
  public ApiResponse<Order> payMock(
      @PathVariable Long orderId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    orderService.getOrderForUser(orderId, currentUser.getId());
    if (!mockPayEnabled) {
      throw new BusinessException("生产环境不允许模拟支付");
    }
    return ApiResponse.ok(withMerchantId(orderService.markPaid(orderId)));
  }

  @PostMapping("/{orderId}/pay/wechat-miniapp")
  public ApiResponse<PrepayWithRequestPaymentResponse> payWechatMiniapp(
      @PathVariable Long orderId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    Order order = orderService.getOrderForUser(orderId, currentUser.getId());

    if (!"PENDING".equals(order.getStatus())) {
      throw new BusinessException("当前订单状态不可发起支付");
    }
    if (currentUser.getMiniappOpenid() == null || currentUser.getMiniappOpenid().isBlank()) {
      throw new BusinessException("请在小程序内重新登录后再发起支付");
    }

    String description = "Zhixi order " + order.getOrderNo();
    return ApiResponse.ok(wechatPayService.createJsapiOrder(order, currentUser.getMiniappOpenid(), description));
  }

  @PostMapping("/{orderId}/pay/wechat-native")
  public ApiResponse<PrepayResponse> payWechatNative(
      @PathVariable Long orderId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    Order order = orderService.getOrderForUser(orderId, currentUser.getId());

    if (!"PENDING".equals(order.getStatus())) {
      throw new BusinessException("当前订单状态不可发起支付");
    }

    String description = "Zhixi order " + order.getOrderNo();
    return ApiResponse.ok(wechatPayService.createNativeOrder(order, description));
  }

  @GetMapping("/user/{userId}")
  public ApiResponse<List<Order>> listByUser(
      @PathVariable Long userId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    if (!currentUser.getId().equals(userId)) {
      throw new BusinessException("无权查看其他用户订单");
    }
    return ApiResponse.ok(withMerchantId(orderService.listByUser(currentUser.getId())));
  }

  @GetMapping("/{orderId}")
  public ApiResponse<Order> getById(
      @PathVariable Long orderId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(withMerchantId(orderService.getOrderForUser(orderId, currentUser.getId())));
  }

  @PostMapping("/{orderId}/trade-management/sync")
  public ApiResponse<WechatTradeManagementService.TradeManagementSyncResult> syncTradeManagementStatus(
      @PathVariable Long orderId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    Order order = orderService.getOrderForUser(orderId, currentUser.getId());
    WechatTradeManagementService.TradeManagementSyncResult result = wechatTradeManagementService.syncOrderStatus(order);
    withMerchantId(result.order());
    return ApiResponse.ok(result);
  }

  @PostMapping("/{orderId}/refund-request")
  public ApiResponse<Order> createRefundRequest(
      @PathVariable Long orderId,
      @Valid @RequestBody UserRefundRequestCreateRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    orderService.createRefundRequest(orderId, currentUser.getId(), request.getReason());
    Order updated = orderService.getOrderForUser(orderId, currentUser.getId());
    return ApiResponse.ok(withMerchantId(updated));
  }

  private List<Order> withMerchantId(List<Order> orders) {
    if (orders == null) {
      return List.of();
    }
    orders.forEach(this::withMerchantId);
    return orders;
  }

  private Order withMerchantId(Order order) {
    if (order != null && payMchid != null && !payMchid.isBlank()) {
      order.setMerchantId(payMchid.trim());
    }
    return order;
  }

  private User requireUser(String authorization) {
    return userAuthService.getUserByToken(extractToken(authorization));
  }

  private String extractToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return "";
    }
    String prefix = "Bearer ";
    return authorization.startsWith(prefix) ? authorization.substring(prefix.length()).trim() : authorization.trim();
  }
}
