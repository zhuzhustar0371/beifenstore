package com.zhixi.backend.dto;

import java.time.LocalDateTime;

public class InviteProductProgressItem {
  private Long productId;
  private String productName;
  private String productImageUrl;
  private LocalDateTime firstPaidAt;

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductImageUrl() {
    return productImageUrl;
  }

  public void setProductImageUrl(String productImageUrl) {
    this.productImageUrl = productImageUrl;
  }

  public LocalDateTime getFirstPaidAt() {
    return firstPaidAt;
  }

  public void setFirstPaidAt(LocalDateTime firstPaidAt) {
    this.firstPaidAt = firstPaidAt;
  }
}
