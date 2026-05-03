package com.zhixi.backend.model;

import java.time.LocalDateTime;

public class InviteProductRelation {
  private Long id;
  private Long inviterId;
  private Long inviteeId;
  private Long productId;
  private LocalDateTime boundAt;
  private LocalDateTime firstPaidAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getInviterId() {
    return inviterId;
  }

  public void setInviterId(Long inviterId) {
    this.inviterId = inviterId;
  }

  public Long getInviteeId() {
    return inviteeId;
  }

  public void setInviteeId(Long inviteeId) {
    this.inviteeId = inviteeId;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
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
