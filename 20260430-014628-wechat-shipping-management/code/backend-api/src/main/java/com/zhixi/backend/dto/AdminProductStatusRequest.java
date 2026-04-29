package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotNull;

public class AdminProductStatusRequest {
  @NotNull(message = "状态不能为空")
  private Boolean active;

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
