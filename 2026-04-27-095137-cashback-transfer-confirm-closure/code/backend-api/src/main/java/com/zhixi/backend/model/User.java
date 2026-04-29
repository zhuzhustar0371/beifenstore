package com.zhixi.backend.model;

import java.time.LocalDateTime;

public class User {
  private Long id;
  private String phone;
  private String passwordHash;
  private String nickname;
  private String inviteCode;
  private Long inviterId;
  private Integer status;
  private String miniappOpenid;
  private LocalDateTime cashbackResetAt;
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash =REMOTE_BACKUP_REDACTED
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getMiniappOpenid() {
    return miniappOpenid;
  }

  public void setMiniappOpenid(String miniappOpenid) {
    this.miniappOpenid = miniappOpenid;
  }

  public LocalDateTime getCashbackResetAt() {
    return cashbackResetAt;
  }

  public void setCashbackResetAt(LocalDateTime cashbackResetAt) {
    this.cashbackResetAt = cashbackResetAt;
  }
}

