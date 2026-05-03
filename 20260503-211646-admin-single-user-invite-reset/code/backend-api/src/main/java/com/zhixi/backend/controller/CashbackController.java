package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.WithdrawalRequest;
import com.zhixi.backend.service.CashbackService;
import com.zhixi.backend.service.ProductService;
import com.zhixi.backend.service.UserAuthService;
import com.zhixi.backend.service.WithdrawalRequestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cashbacks")
public class CashbackController {
  private final CashbackService cashbackService;
  private final ProductService productService;
  private final UserAuthService userAuthService;
  private final WithdrawalRequestService withdrawalRequestService;

  public CashbackController(
      CashbackService cashbackService,
      ProductService productService,
      UserAuthService userAuthService,
      WithdrawalRequestService withdrawalRequestService
  ) {
    this.cashbackService = cashbackService;
    this.productService = productService;
    this.userAuthService = userAuthService;
    this.withdrawalRequestService = withdrawalRequestService;
  }

  @GetMapping("/me/withdrawals")
  public ApiResponse<List<WithdrawalRequest>> myWithdrawals(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(withdrawalRequestService.listByUser(currentUser.getId()));
  }

  @PostMapping("/me/withdrawals")
  public ApiResponse<WithdrawalRequest> createWithdrawal(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody(required = false) Map<String, Object> payload
  ) {
    User currentUser = requireUser(authorization);
    String idempotencyKey = null;
    if (payload != null && payload.get("idempotencyKey") != null) {
      idempotencyKey = payload.get("idempotencyKey").toString().trim();
    }
    String applyMode = null;
    if (payload != null && payload.get("applyMode") != null) {
      applyMode = payload.get("applyMode").toString().trim();
    }
    return ApiResponse.ok(withdrawalRequestService.createUserRequest(currentUser.getId(), idempotencyKey, applyMode));
  }

  @GetMapping("/me/summary")
  public ApiResponse<Map<String, Object>> mySummary(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    Map<String, Object> payload = new LinkedHashMap<>(cashbackService.buildUserSummary(currentUser.getId()));
    payload.putAll(withdrawalRequestService.buildUserPreview(currentUser.getId()));
    return ApiResponse.ok(payload);
  }

  @GetMapping("/{userId}")
  public ApiResponse<List<CashbackRecord>> listByUser(@PathVariable Long userId) {
    return ApiResponse.ok(cashbackService.listByUser(userId));
  }

  @GetMapping("/{cashbackId}/merchant-transfer/confirm-params")
  public ApiResponse<Map<String, Object>> merchantTransferConfirmParams(
      @PathVariable Long cashbackId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(cashbackService.buildMerchantTransferConfirmParamsForUser(cashbackId, currentUser.getId()));
  }

  @PostMapping("/{cashbackId}/transfer/sync")
  public ApiResponse<CashbackRecord> syncMerchantTransfer(
      @PathVariable Long cashbackId,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(cashbackService.syncTransferForUser(cashbackId, currentUser.getId()));
  }

  @GetMapping("/rules")
  public ApiResponse<Map<String, Object>> rules(@RequestParam(required = false) Long productId) {
    Product ruleProduct = resolveRuleProduct(productId);
    return ApiResponse.ok(cashbackService.buildRules(ruleProduct));
  }

  private Product resolveRuleProduct(Long productId) {
    if (productId != null) {
      return productService.getById(productId);
    }
    return productService.getPreferredActive();
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
