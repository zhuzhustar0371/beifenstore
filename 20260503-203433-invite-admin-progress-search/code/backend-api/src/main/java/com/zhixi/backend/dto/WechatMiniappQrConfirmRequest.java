package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class WechatMiniappQrConfirmRequest {
  @NotBlank(message = "scene不能为空")
  private String scene;

  @NotBlank(message = "code不能为空")
  private String code;

  private Long inviterId;
  private String nickName;
  private String avatarUrl;

  public String getScene() {
    return scene;
  }

  public void setScene(String scene) {
    this.scene = scene;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Long getInviterId() {
    return inviterId;
  }

  public void setInviterId(Long inviterId) {
    this.inviterId = inviterId;
  }

  public String getNickName() {
    return nickName;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }
}
