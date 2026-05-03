package com.zhixi.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
  private Long id;
  private String orderNo;
  private Long userId;
  private Long productId;
  private String productName;
  private String productImageUrl;
  private Integer quantity;
  private BigDecimal productAmount;
  private BigDecimal shippingFee;
  private BigDecimal cashbackBaseAmount;
  private BigDecimal totalAmount;
  private String status;
  private String recipientName;
  private String recipientPhone;
  private String province;
  private String city;
  private String district;
  private String address;
  private String trackingNo;
  private String payType;
  private String transactionId;
  private String merchantId;
  private String refundStatus = "NONE";
  private String refundNo;
  private String refundId;
  private LocalDateTime refundApplyAt;
  private LocalDateTime refundCompletedAt;
  private String refundRequestStatus = "NONE";
  private String refundRequestReason;
  private LocalDateTime refundRequestAt;
  private LocalDateTime refundReviewAt;
  private String refundReviewRemark;
  private Long refundReviewAdminId;
  private LocalDateTime createdAt;
  private LocalDateTime paidAt;
  private LocalDateTime completedAt;
  private LocalDateTime cancelTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(String orderNo) {
    this.orderNo = orderNo;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductImageUrl() {
    return productImageUrl;
  }

  public void setProductImageUrl(String productImageUrl) {
    this.productImageUrl = productImageUrl;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getProductAmount() {
    return productAmount;
  }

  public void setProductAmount(BigDecimal productAmount) {
    this.productAmount = productAmount;
  }

  public BigDecimal getShippingFee() {
    return shippingFee;
  }

  public void setShippingFee(BigDecimal shippingFee) {
    this.shippingFee = shippingFee;
  }

  public BigDecimal getCashbackBaseAmount() {
    return cashbackBaseAmount;
  }

  public void setCashbackBaseAmount(BigDecimal cashbackBaseAmount) {
    this.cashbackBaseAmount = cashbackBaseAmount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public void setRecipientName(String recipientName) {
    this.recipientName = recipientName;
  }

  public String getRecipientPhone() {
    return recipientPhone;
  }

  public void setRecipientPhone(String recipientPhone) {
    this.recipientPhone = recipientPhone;
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getDistrict() {
    return district;
  }

  public void setDistrict(String district) {
    this.district = district;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getTrackingNo() {
    return trackingNo;
  }

  public void setTrackingNo(String trackingNo) {
    this.trackingNo = trackingNo;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public LocalDateTime getCancelTime() {
    return cancelTime;
  }

  public void setCancelTime(LocalDateTime cancelTime) {
    this.cancelTime = cancelTime;
  }

  public String getPayType() {
    return payType;
  }

  public void setPayType(String payType) {
    this.payType = payType;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getRefundStatus() {
    return refundStatus;
  }

  public void setRefundStatus(String refundStatus) {
    this.refundStatus = refundStatus;
  }

  public String getRefundNo() {
    return refundNo;
  }

  public void setRefundNo(String refundNo) {
    this.refundNo = refundNo;
  }

  public String getRefundId() {
    return refundId;
  }

  public void setRefundId(String refundId) {
    this.refundId = refundId;
  }

  public LocalDateTime getRefundApplyAt() {
    return refundApplyAt;
  }

  public void setRefundApplyAt(LocalDateTime refundApplyAt) {
    this.refundApplyAt = refundApplyAt;
  }

  public LocalDateTime getRefundCompletedAt() {
    return refundCompletedAt;
  }

  public void setRefundCompletedAt(LocalDateTime refundCompletedAt) {
    this.refundCompletedAt = refundCompletedAt;
  }

  public String getRefundRequestStatus() {
    return refundRequestStatus;
  }

  public void setRefundRequestStatus(String refundRequestStatus) {
    this.refundRequestStatus = refundRequestStatus;
  }

  public String getRefundRequestReason() {
    return refundRequestReason;
  }

  public void setRefundRequestReason(String refundRequestReason) {
    this.refundRequestReason = refundRequestReason;
  }

  public LocalDateTime getRefundRequestAt() {
    return refundRequestAt;
  }

  public void setRefundRequestAt(LocalDateTime refundRequestAt) {
    this.refundRequestAt = refundRequestAt;
  }

  public LocalDateTime getRefundReviewAt() {
    return refundReviewAt;
  }

  public void setRefundReviewAt(LocalDateTime refundReviewAt) {
    this.refundReviewAt = refundReviewAt;
  }

  public String getRefundReviewRemark() {
    return refundReviewRemark;
  }

  public void setRefundReviewRemark(String refundReviewRemark) {
    this.refundReviewRemark = refundReviewRemark;
  }

  public Long getRefundReviewAdminId() {
    return refundReviewAdminId;
  }

  public void setRefundReviewAdminId(Long refundReviewAdminId) {
    this.refundReviewAdminId = refundReviewAdminId;
  }
}
