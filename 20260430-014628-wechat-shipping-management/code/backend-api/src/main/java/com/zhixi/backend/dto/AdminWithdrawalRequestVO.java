package com.zhixi.backend.dto;

import com.zhixi.backend.model.WithdrawalRequest;

public class AdminWithdrawalRequestVO extends WithdrawalRequest {
  private String userNickname;
  private String userAvatarUrl;

  public AdminWithdrawalRequestVO() {
  }

  public AdminWithdrawalRequestVO(WithdrawalRequest request) {
    setId(request.getId());
    setUserId(request.getUserId());
    setAmount(request.getAmount());
    setRequestedAmount(request.getRequestedAmount());
    setStatus(request.getStatus());
    setSource(request.getSource());
    setRemark(request.getRemark());
    setIdempotencyKey(request.getIdempotencyKey());
    setRequestNo(request.getRequestNo());
    setApplyMode(request.getApplyMode());
    setSuggestedAmount(request.getSuggestedAmount());
    setReadyAmount(request.getReadyAmount());
    setPendingAmount(request.getPendingAmount());
    setCreatedAt(request.getCreatedAt());
    setApprovedAt(request.getApprovedAt());
    setCompletedAt(request.getCompletedAt());
    setEarliestEligibleAt(request.getEarliestEligibleAt());
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
