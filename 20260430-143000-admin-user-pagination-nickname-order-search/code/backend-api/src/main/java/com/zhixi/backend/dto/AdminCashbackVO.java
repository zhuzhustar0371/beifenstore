package com.zhixi.backend.dto;

import com.zhixi.backend.model.CashbackRecord;

public class AdminCashbackVO extends CashbackRecord {
  private String userNickname;
  private String userAvatarUrl;

  public AdminCashbackVO() {
  }

  public AdminCashbackVO(CashbackRecord record) {
    setId(record.getId());
    setUserId(record.getUserId());
    setOrderId(record.getOrderId());
    setType(record.getType());
    setAmount(record.getAmount());
    setBatchNo(record.getBatchNo());
    setStatus(record.getStatus());
    setRemark(record.getRemark());
    setOutBatchNo(record.getOutBatchNo());
    setOutDetailNo(record.getOutDetailNo());
    setTransferId(record.getTransferId());
    setTransferDetailId(record.getTransferDetailId());
    setTransferFailReason(record.getTransferFailReason());
    setTransferPackageInfo(record.getTransferPackageInfo());
    setTransferTime(record.getTransferTime());
    setEligibleAt(record.getEligibleAt());
    setWithdrawalRequestId(record.getWithdrawalRequestId());
    setEarlyWithdrawal(record.getEarlyWithdrawal());
    setCreatedAt(record.getCreatedAt());
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
