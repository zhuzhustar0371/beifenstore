package com.zhixi.backend.dto;

import java.time.LocalDateTime;

public class AdminUserVO {
  private Long id;
  private String phone;
  private String nickname;
  private String inviteCode;
  private Long inviterId;
  private Integer status;
  private String wechatWebOpenid;
  private String wechatMiniappOpenid;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
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

  public Long getInviterId() {
    return inviterId;
  }

  public void setInviterId(Long inviterId) {
    this.inviterId = inviterId;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getWechatWebOpenid() {
    return wechatWebOpenid;
  }

  public void setWechatWebOpenid(String wechatWebOpenid) {
    this.wechatWebOpenid = wechatWebOpenid;
  }

  public String getWechatMiniappOpenid() {
    return wechatMiniappOpenid;
  }

  public void setWechatMiniappOpenid(String wechatMiniappOpenid) {
    this.wechatMiniappOpenid = wechatMiniappOpenid;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
