package com.zhixi.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AdminInviteProgressVO {
  private Long id;
  private String inviteNo;
  private Long inviterId;
  private String inviterNickname;
  private String inviterAvatarUrl;
  private Long inviteeId;
  private String inviteeNickname;
  private String inviteeAvatarUrl;
  private LocalDateTime boundAt;
  private int firstPaidCount;
  private int totalProductCount;
  private List<InviteProductProgressItem> products;

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

  public int getFirstPaidCount() {
    return firstPaidCount;
  }

  public void setFirstPaidCount(int firstPaidCount) {
    this.firstPaidCount = firstPaidCount;
  }

  public int getTotalProductCount() {
    return totalProductCount;
  }

  public void setTotalProductCount(int totalProductCount) {
    this.totalProductCount = totalProductCount;
  }

  public List<InviteProductProgressItem> getProducts() {
    return products;
  }

  public void setProducts(List<InviteProductProgressItem> products) {
    this.products = products;
  }
}
