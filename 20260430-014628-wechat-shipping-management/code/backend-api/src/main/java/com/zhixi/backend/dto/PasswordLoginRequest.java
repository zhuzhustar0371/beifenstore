package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PasswordLoginRequest {
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
  private String phone;

  @NotBlank(message = "密码不能为空")
  private String password;

  @NotBlank(message = "请完成图片验证码")
  private String captchaId;

  @NotBlank(message = "请输入图片验证码")
  private String captchaCode;

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getCaptchaId() { return captchaId; }
  public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
  public String getCaptchaCode() { return captchaCode; }
  public void setCaptchaCode(String captchaCode) { this.captchaCode = captchaCode; }
}
