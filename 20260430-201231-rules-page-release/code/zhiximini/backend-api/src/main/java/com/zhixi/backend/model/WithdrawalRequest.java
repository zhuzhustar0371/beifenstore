package com.zhixi.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WithdrawalRequest {
  private Long id;
  private Long userId;
  private BigDecimal amount;
  private BigDecimal requestedAmount;
  private String status;
  private String source;
  private String remark;
  private String idempotencyKey;
  private String requestNo;
  private String applyMode;
  private BigDecimal suggestedAmount;
  private BigDecimal readyAmount;
  private BigDecimal pendingAmount;
  private LocalDateTime createdAt;
  private LocalDateTime approvedAt;
  private LocalDateTime completedAt;
  private LocalDateTime earliestEligibleAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getRequestedAmount() {
    return requestedAmount;
  }

  public void setRequestedAmount(BigDecimal requestedAmount) {
    this.requestedAmount = requestedAmount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public void setRequestNo(String requestNo) {
    this.requestNo = requestNo;
  }

  public String getApplyMode() {
    return applyMode;
  }

  public void setApplyMode(String applyMode) {
    this.applyMode = applyMode;
  }

  public BigDecimal getSuggestedAmount() {
    return suggestedAmount;
  }

  public void setSuggestedAmount(BigDecimal suggestedAmount) {
    this.suggestedAmount = suggestedAmount;
  }

  public BigDecimal getReadyAmount() {
    return readyAmount;
  }

  public void setReadyAmount(BigDecimal readyAmount) {
    this.readyAmount = readyAmount;
  }

  public BigDecimal getPendingAmount() {
    return pendingAmount;
  }

  public void setPendingAmount(BigDecimal pendingAmount) {
    this.pendingAmount = pendingAmount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(LocalDateTime approvedAt) {
    this.approvedAt = approvedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public LocalDateTime getEarliestEligibleAt() {
    return earliestEligibleAt;
  }

  public void setEarliestEligibleAt(LocalDateTime earliestEligibleAt) {
    this.earliestEligibleAt = earliestEligibleAt;
  }
}
