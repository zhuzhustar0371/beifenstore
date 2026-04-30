package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.service.CashbackService;
import com.zhixi.backend.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cashbacks")
public class CashbackController {
  private final CashbackService cashbackService;
  private final ProductService productService;

  public CashbackController(CashbackService cashbackService, ProductService productService) {
    this.cashbackService = cashbackService;
    this.productService = productService;
  }

  @GetMapping("/{userId}")
  public ApiResponse<List<CashbackRecord>> listByUser(@PathVariable Long userId) {
    return ApiResponse.ok(cashbackService.listByUser(userId));
  }

  @GetMapping("/rules")
  public ApiResponse<Map<String, Object>> rules(@RequestParam(required = false) Long productId) {
    Product ruleProduct = resolveRuleProduct(productId);
    return ApiResponse.ok(
        cashbackService.buildRules(
            ruleProduct.getId(),
            ruleProduct.getName(),
            ruleProduct.getPrice()
        )
    );
  }

  private Product resolveRuleProduct(Long productId) {
    if (productId != null) {
      return productService.getById(productId);
    }
    List<Product> activeProducts = productService.listActive();
    if (activeProducts.isEmpty()) {
      throw new BusinessException("暂无可用商品，无法生成返现规则");
    }
    return activeProducts.get(0);
  }
}
