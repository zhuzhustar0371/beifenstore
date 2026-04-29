package com.zhixi.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashbackRecord {
  private Long id;
  private Long userId;
  private Long orderId;
  private String type;
  private BigDecimal amount;
  private Integer batchNo;
  private String status;
  private String remark;
  private String outBatchNo;
  private String outDetailNo;
  private String transferId;
  private String transferDetailId;
  private String transferFailReason;
  private String transferPackageInfo;
  private LocalDateTime transferTime;
  private LocalDateTime eligibleAt;
  private Long withdrawalRequestId;
  private Boolean earlyWithdrawal;
  private LocalDateTime createdAt;

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

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Integer getBatchNo() {
    return batchNo;
  }

  public void setBatchNo(Integer batchNo) {
    this.batchNo = batchNo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getOutBatchNo() {
    return outBatchNo;
  }

  public void setOutBatchNo(String outBatchNo) {
    this.outBatchNo = outBatchNo;
  }

  public String getOutDetailNo() {
    return outDetailNo;
  }

  public void setOutDetailNo(String outDetailNo) {
    this.outDetailNo = outDetailNo;
  }

  public String getTransferId() {
    return transferId;
  }

  public void setTransferId(String transferId) {
    this.transferId = transferId;
  }

  public String getTransferDetailId() {
    return transferDetailId;
  }

  public void setTransferDetailId(String transferDetailId) {
    this.transferDetailId = transferDetailId;
  }

  public String getTransferFailReason() {
    return transferFailReason;
  }

  public void setTransferFailReason(String transferFailReason) {
    this.transferFailReason = transferFailReason;
  }

  public String getTransferPackageInfo() {
    return transferPackageInfo;
  }

  public void setTransferPackageInfo(String transferPackageInfo) {
    this.transferPackageInfo = transferPackageInfo;
  }

  public LocalDateTime getTransferTime() {
    return transferTime;
  }

  public void setTransferTime(LocalDateTime transferTime) {
    this.transferTime = transferTime;
  }

  public LocalDateTime getEligibleAt() {
    return eligibleAt;
  }

  public void setEligibleAt(LocalDateTime eligibleAt) {
    this.eligibleAt = eligibleAt;
  }

  public Long getWithdrawalRequestId() {
    return withdrawalRequestId;
  }

  public void setWithdrawalRequestId(Long withdrawalRequestId) {
    this.withdrawalRequestId = withdrawalRequestId;
  }

  public Boolean getEarlyWithdrawal() {
    return earlyWithdrawal;
  }

  public void setEarlyWithdrawal(Boolean earlyWithdrawal) {
    this.earlyWithdrawal = earlyWithdrawal;
  }
}
