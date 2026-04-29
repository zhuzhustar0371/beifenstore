package com.zhixi.backend.dto;

import java.time.LocalDateTime;

public class AdminInviteVO {
  private Long id;
  private String inviteNo;
  private Long inviterId;
  private String inviterNickname;
  private String inviterAvatarUrl;
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

  public String getInviteNo() {
    return inviteNo;
  }

  public void setInviteNo(String inviteNo) {
    this.inviteNo = inviteNo;
  }

  public Long getInviterId() {
    return inviterId;
  }

  public void setInviterId(Long inviterId) {
    this.inviterId = inviterId;
  }

  public String getInviterNickname() {
    return inviterNickname;
  }

  public void setInviterNickname(String inviterNickname) {
    this.inviterNickname = inviterNickname;
  }

  public String getInviterAvatarUrl() {
    return inviterAvatarUrl;
  }

  public void setInviterAvatarUrl(String inviterAvatarUrl) {
    this.inviterAvatarUrl = inviterAvatarUrl;
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
