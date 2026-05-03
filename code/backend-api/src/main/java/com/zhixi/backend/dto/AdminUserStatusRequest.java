package com.zhixi.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AdminUserStatusRequest {
  @NotNull(message = "用户状态不能为空")
  @Min(value = 0, message = "状态值错误")
  @Max(value = 1, message = "状态值错误")
  private Integer status;

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }
}
