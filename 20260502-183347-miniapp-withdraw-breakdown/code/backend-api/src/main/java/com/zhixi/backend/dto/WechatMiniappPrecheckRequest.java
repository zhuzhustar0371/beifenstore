package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class WechatMiniappPrecheckRequest {
  @NotBlank(message = "code cannot be empty")
  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
