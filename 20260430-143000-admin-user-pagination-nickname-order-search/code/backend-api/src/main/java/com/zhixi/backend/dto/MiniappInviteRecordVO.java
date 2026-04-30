package com.zhixi.backend.dto;

import java.time.LocalDateTime;

public class MiniappInviteRecordVO {
  private Long id;
  private Long inviteeId;
  private String inviteeNickname;
  private String inviteeAvatarUrl;
  private LocalDateTime boundAt;
  private LocalDateTime firstPaidAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getInviteeId() {
    return inviteeId;
  }

  public void setInviteeId(Long inviteeId) {
    this.inviteeId = inviteeId;
  }

  public String getInviteeNickname() {
    return inviteeNickname;
  }

  public void setInviteeNickname(String inviteeNickname) {
    this.inviteeNickname = inviteeNickname;
  }

  public String getInviteeAvatarUrl() {
    return inviteeAvatarUrl;
  }

  public void setInviteeAvatarUrl(String inviteeAvatarUrl) {
    this.inviteeAvatarUrl = inviteeAvatarUrl;
  }

  public LocalDateTime getBoundAt() {
    return boundAt;
  }

  public void setBoundAt(LocalDateTime boundAt) {
    this.boundAt = boundAt;
  }

  public LocalDateTime getFirstPaidAt() {
    return firstPaidAt;
  }

  public void setFirstPaidAt(LocalDateTime firstPaidAt) {
    this.firstPaidAt = firstPaidAt;
  }
}
