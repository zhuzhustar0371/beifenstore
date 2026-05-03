package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRefundRequestCreateRequest {

  @NotBlank(message = "退款理由不能为空")
  @Size(max = 200, message = "退款理由最长200字")
  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
