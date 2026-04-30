package com.zhixi.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WithdrawalRequestItem {
  private Long id;
  private Long requestId;
  private Long cashbackId;
  private BigDecimal amount;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public Long getCashbackId() {
    return cashbackId;
  }

  public void setCashbackId(Long cashbackId) {
    this.cashbackId = cashbackId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
