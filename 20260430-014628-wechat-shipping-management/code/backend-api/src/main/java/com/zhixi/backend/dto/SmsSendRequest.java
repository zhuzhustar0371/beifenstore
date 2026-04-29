package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SmsSendRequest {
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
  private String phone;

  @Pattern(regexp = "^(REGISTER|LOGIN|RESET_PASSWORD)?$", message = "短信场景不支持")
  private String scene;

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getScene() {
    return scene;
  }

  public void setScene(String scene) {
    this.scene = scene;
  }
}
