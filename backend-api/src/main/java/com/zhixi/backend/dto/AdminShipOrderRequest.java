package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminShipOrderRequest {
  @NotBlank(message = "物流单号不能为空")
  private String trackingNo;

  public String getTrackingNo() {
    return trackingNo;
  }

  public void setTrackingNo(String trackingNo) {
    this.trackingNo = trackingNo;
  }
}
