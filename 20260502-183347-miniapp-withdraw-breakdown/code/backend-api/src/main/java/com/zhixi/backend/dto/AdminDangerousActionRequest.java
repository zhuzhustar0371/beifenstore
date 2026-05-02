package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminDangerousActionRequest {
  @NotBlank(message = "管理员密码不能为空")
  private String adminPassword;

  public String getAdminPassword() {
    return adminPassword;
  }

  public void setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
  }
}
