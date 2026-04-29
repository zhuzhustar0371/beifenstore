package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.dto.AdminPageResult;
import com.zhixi.backend.dto.AdminProductStatusRequest;
import com.zhixi.backend.dto.AdminProductUpsertRequest;
import com.zhixi.backend.dto.AdminShipOrderRequest;
import com.zhixi.backend.dto.AdminUserStatusRequest;
import com.zhixi.backend.dto.AdminUserVO;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.AdminOperationLog;
import com.zhixi.backend.model.Order;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.model.WithdrawalRequest;
import com.zhixi.backend.service.AdminAuditService;
import com.zhixi.backend.service.AdminManageService;
import com.zhixi.backend.service.CashbackService;
import com.zhixi.backend.service.OrderService;
import com.zhixi.backend.service.UserService;
import com.zhixi.backend.service.WithdrawalRequestService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final UserService userService;
  private final OrderService orderService;
  private final CashbackService cashbackService;
  private final AdminManageService adminManageService;
  private final AdminAuditService adminAuditService;
  private final WithdrawalRequestService withdrawalRequestService;

  public AdminController(
      UserService userService,
      OrderService orderService,
      CashbackService cashbackService,
      AdminManageService adminManageService,
      AdminAuditService adminAuditService,
      WithdrawalRequestService withdrawalRequestService
  ) {
    this.userService = userService;
    this.orderService = orderService;
    this.cashbackService = cashbackService;
    this.adminManageService = adminManageService;
    this.adminAuditService = adminAuditService;
    this.withdrawalRequestService = withdrawalRequestService;
  }

  @GetMapping("/dashboard")
  public ApiResponse<Map<String, Object>> dashboard() {
    Map<String, Object> data = new HashMap<>();
    data.put("totalUsers", userService.totalUsers());
    data.put("todayUsers", adminManageService.todayUsers());
    data.put("todayOrders", adminManageService.todayOrders());
    data.put("pendingShipments", adminManageService.pendingShipments());
    data.put("totalIncome", orderService.totalPaidAmount());
    data.put("todayIncome", adminManageService.todayIncome());
    data.put("totalCashback", cashbackService.totalCashbackAmount());
    data.put("todayCashback", adminManageService.todayCashback());
    return ApiResponse.ok(data);
  }

  @GetMapping("/orders")
  public ApiResponse<AdminPageResult<Order>> orders(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ApiResponse.ok(adminManageService.pageOrders(status, userId, keyword, page, size));
  }

  @GetMapping("/cashbacks")
  public ApiResponse<AdminPageResult<CashbackRecord>> cashbacks(
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ApiResponse.ok(adminManageService.pageCashbacks(userId, type, status, page, size));
  }

  @GetMapping("/withdrawals")
  public ApiResponse<AdminPageResult<WithdrawalRequest>> withdrawals(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ApiResponse.ok(withdrawalRequestService.pageAdmin(status, page, size));
  }

  @GetMapping(value = "/withdrawals/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter withdrawalEvents() {
    return withdrawalRequestService.subscribe();
  }

  @GetMapping("/users")
  public ApiResponse<AdminPageResult<AdminUserVO>> users(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ApiResponse.ok(adminManageService.pageUsers(keyword, status, page, size));
  }

  @GetMapping("/products")
  public ApiResponse<AdminPageResult<Product>> products(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ApiResponse.ok(adminManageService.pageProducts(keyword, active, page, size));
  }

  @GetMapping("/invites")
  public ApiResponse<AdminPageResult<InviteRelation>> invites(
      @RequestParam(required = false) Long inviterId,
      @RequestParam(required = false) Long inviteeId,
      @RequestParam(required = false) Boolean firstPaidOnly,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ApiResponse.ok(adminManageService.pageInvites(inviterId, inviteeId, firstPaidOnly, page, size));
  }

  @GetMapping("/audit-logs")
  public ApiResponse<AdminPageResult<AdminOperationLog>> auditLogs(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ApiResponse.ok(adminAuditService.page(page, size));
  }

  @PostMapping("/products")
  public ApiResponse<Product> createProduct(@Valid @RequestBody AdminProductUpsertRequest request) {
    return ApiResponse.ok(adminManageService.createProduct(request));
  }

  @PutMapping("/products/{productId}")
  public ApiResponse<Product> updateProduct(
      @PathVariable Long productId,
      @Valid @RequestBody AdminProductUpsertRequest request
  ) {
    return ApiResponse.ok(adminManageService.updateProduct(productId, request));
  }

  @DeleteMapping("/products/{productId}")
  public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
    adminManageService.deleteProduct(productId);
    return ApiResponse.ok(null);
  }

  @PutMapping("/products/{productId}/status")
  public ApiResponse<Void> updateProductStatus(
      @PathVariable Long productId,
      @Valid @RequestBody AdminProductStatusRequest request
  ) {
    adminManageService.updateProductStatus(productId, request.getActive());
    return ApiResponse.ok(null);
  }

  @PutMapping("/users/{userId}/status")
  public ApiResponse<Void> updateUserStatus(
      @PathVariable Long userId,
      @Valid @RequestBody AdminUserStatusRequest request
  ) {
    adminManageService.updateUserStatus(userId, request.getStatus());
    return ApiResponse.ok(null);
  }

  @PostMapping("/orders/{orderId}/ship")
  public ApiResponse<Void> shipOrder(
      @PathVariable Long orderId,
      @Valid @RequestBody AdminShipOrderRequest request
  ) {
    adminManageService.shipOrder(orderId, request.getTrackingNo(), request.getExpressCompany());
    return ApiResponse.ok(null);
  }

  @PostMapping("/orders/{orderId}/refund")
  public ApiResponse<Void> refundOrder(
      @PathVariable Long orderId,
      @RequestBody Map<String, String> payload
  ) {
    adminManageService.refundOrder(orderId, payload.getOrDefault("reason", "协商退款"));
    return ApiResponse.ok(null);
  }

  @PostMapping("/cashbacks/{cashbackId}/transfer")
  public ApiResponse<CashbackRecord> transferCashback(
      @PathVariable Long cashbackId
  ) {
    return ApiResponse.ok(adminManageService.transferCashback(cashbackId));
  }

  @PostMapping("/withdrawals/{requestId}/approve")
  public ApiResponse<WithdrawalRequest> approveWithdrawal(
      @PathVariable Long requestId
  ) {
    return ApiResponse.ok(adminManageService.approveWithdrawalRequest(requestId));
  }

  @PostMapping("/cashbacks/{cashbackId}/transfer/sync")
  public ApiResponse<CashbackRecord> syncCashbackTransfer(
      @PathVariable Long cashbackId
  ) {
    return ApiResponse.ok(adminManageService.syncCashbackTransfer(cashbackId));
  }

  @PostMapping("/cashbacks/reset-all")
  public ApiResponse<Map<String, Object>> resetAllCashbackStats() {
    return ApiResponse.ok(adminManageService.resetAllCashbackStats());
  }

  @PostMapping("/users/reset-all")
  public ApiResponse<Map<String, Object>> resetAllUsers() {
    return ApiResponse.ok(adminManageService.resetAllUsers());
  }
}
