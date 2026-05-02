package com.zhixi.backend.dto;

import java.math.BigDecimal;

public class AdminRefundApproveRequest {

  private BigDecimal amount;
  private String adminRemark;

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getAdminRemark() {
    return adminRemark;
  }

  public void setAdminRemark(String adminRemark) {
    this.adminRemark = adminRemark;
  }
}
