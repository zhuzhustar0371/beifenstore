package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminLoginRequest {
  @NotBlank(message = "璐﹀彿涓嶈兘涓虹┖")
  private String username;

  @NotBlank(message = "瀵嗙爜涓嶈兘涓虹┖")
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password =REMOTE_BACKUP_REDACTED
  }
}

