package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class WechatMockScanRequest {
  @NotBlank(message = "场景码不能为空")
  private String scene;

  @NotBlank(message = "openid不能为空")
  private String openid;

  private String nickname;

  public String getScene() {
    return scene;
  }

  public void setScene(String scene) {
    this.scene = scene;
  }

  public String getOpenid() {
    return openid;
  }

  public void setOpenid(String openid) {
    this.openid = openid;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }
}
