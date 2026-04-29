package com.zhixi.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashbackDebt {
  private Long id;
  private Long userId;
  private Long orderId;
  private Long cashbackId;
  private BigDecimal amount;
  private String reason;
  private String status;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }

  public Long getOrderId() { return orderId; }
  public void setOrderId(Long orderId) { this.orderId = orderId; }

  public Long getCashbackId() { return cashbackId; }
  public void setCashbackId(Long cashbackId) { this.cashbackId = cashbackId; }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }

  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
