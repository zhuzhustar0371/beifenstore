package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminShipOrderRequest {
  @NotBlank(message = "物流单号不能为空")
  private String trackingNo;

  @NotBlank(message = "物流公司编码不能为空")
  private String expressCompany;

  public String getTrackingNo() {
    return trackingNo;
  }

  public void setTrackingNo(String trackingNo) {
    this.trackingNo = trackingNo;
  }

  public String getExpressCompany() {
    return expressCompany;
  }

  public void setExpressCompany(String expressCompany) {
    this.expressCompany = expressCompany;
  }
}
