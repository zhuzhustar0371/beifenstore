package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
  @NotBlank(message = "鎵嬫満鍙蜂笉鑳戒负绌?)
  @Pattern(regexp = "^1\\d{10}$", message = "鎵嬫満鍙锋牸寮忎笉姝ｇ‘")
  private String phone;

  @NotBlank(message = "鐭俊楠岃瘉鐮佷笉鑳戒负绌?)
  private String smsCode;

  @NotBlank(message = "瀵嗙爜涓嶈兘涓虹┖")
  @Size(min = 6, max = 32, message = "瀵嗙爜闀垮害闇€鍦?-32浣嶄箣闂?)
  private String newPassword;

  @NotBlank(message = "璇峰畬鎴愬浘鐗囬獙璇佺爜")
  private String captchaId;

  @NotBlank(message = "璇疯緭鍏ュ浘鐗囬獙璇佺爜")
  private String captchaCode;

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getSmsCode() {
    return smsCode;
  }

  public void setSmsCode(String smsCode) {
    this.smsCode = smsCode;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword =REMOTE_BACKUP_REDACTED
  }

  public String getCaptchaId() {
    return captchaId;
  }

  public void setCaptchaId(String captchaId) {
    this.captchaId = captchaId;
  }

  public String getCaptchaCode() {
    return captchaCode;
  }

  public void setCaptchaCode(String captchaCode) {
    this.captchaCode = captchaCode;
  }
}

