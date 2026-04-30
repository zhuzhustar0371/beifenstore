package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
  private String phone;

  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
  private String password;

  @NotBlank(message = "短信验证码不能为空")
  private String smsCode;

  @NotBlank(message = "请完成图片验证码")
  private String captchaId;

  @NotBlank(message = "请输入图片验证码")
  private String captchaCode;

  private String nickname;
  private String inviteCode;

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getSmsCode() { return smsCode; }
  public void setSmsCode(String smsCode) { this.smsCode = smsCode; }
  public String getCaptchaId() { return captchaId; }
  public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
  public String getCaptchaCode() { return captchaCode; }
  public void setCaptchaCode(String captchaCode) { this.captchaCode = captchaCode; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getInviteCode() { return inviteCode; }
  public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
