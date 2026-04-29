package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class WechatBindMobileRequest {
  @NotBlank(message = "场景码不能为空")
  private String scene;

  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
  private String phone;

  @NotBlank(message = "验证码不能为空")
  private String code;

  private String nickname;
  private String inviteCode;

  public String getScene() {
    return scene;
  }

  public void setScene(String scene) {
    this.scene = scene;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }
}
