package com.zhixi.backend.dto;

import com.zhixi.backend.model.Order;

public class AdminOrderVO extends Order {
  private String userNickname;
  private String userAvatarUrl;

  public AdminOrderVO() {
  }

  public AdminOrderVO(Order order) {
    setId(order.getId());
    setOrderNo(order.getOrderNo());
    setUserId(order.getUserId());
    setProductId(order.getProductId());
    setProductName(order.getProductName());
    setProductImageUrl(order.getProductImageUrl());
    setQuantity(order.getQuantity());
    setTotalAmount(order.getTotalAmount());
    setStatus(order.getStatus());
    setRecipientName(order.getRecipientName());
    setRecipientPhone(order.getRecipientPhone());
    setAddress(order.getAddress());
    setTrackingNo(order.getTrackingNo());
    setPayType(order.getPayType());
    setTransactionId(order.getTransactionId());
    setMerchantId(order.getMerchantId());
    setRefundStatus(order.getRefundStatus());
    setRefundNo(order.getRefundNo());
    setRefundId(order.getRefundId());
    setRefundApplyAt(order.getRefundApplyAt());
    setCreatedAt(order.getCreatedAt());
    setPaidAt(order.getPaidAt());
    setCompletedAt(order.getCompletedAt());
  }

  public String getUserNickname() {
    return userNickname;
  }

  public void setUserNickname(String userNickname) {
    this.userNickname = userNickname;
  }

  public String getUserAvatarUrl() {
    return userAvatarUrl;
  }

  public void setUserAvatarUrl(String userAvatarUrl) {
    this.userAvatarUrl = userAvatarUrl;
  }
}
