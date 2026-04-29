package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  public ApiResponse<List<Product>> list() {
    return ApiResponse.ok(productService.listActive());
  }

  @GetMapping("/{id}")
  public ApiResponse<Product> getById(@PathVariable Long id) {
    return ApiResponse.ok(productService.getById(id));
  }
}
