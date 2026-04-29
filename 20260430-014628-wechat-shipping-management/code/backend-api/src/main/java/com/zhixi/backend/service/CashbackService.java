package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.mapper.CashbackRecordMapper;
import com.zhixi.backend.model.CashbackRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CashbackService {
  private static final BigDecimal PERSONAL_SECOND_RATIO = BigDecimal.valueOf(0.10);
  private static final BigDecimal PERSONAL_THIRD_RATIO = BigDecimal.valueOf(0.20);
  private static final BigDecimal PERSONAL_FOURTH_RATIO = BigDecimal.ONE;
  private static final BigDecimal INVITE_FIRST_ORDER_RATIO = BigDecimal.ONE;
  private static final BigDecimal INVITE_FIRST_BATCH_RATIO = BigDecimal.ONE;
  private static final BigDecimal INVITE_REPEAT_RATIO = BigDecimal.valueOf(0.20);
  private static final long WITHDRAWAL_WAIT_DAYS = 7L;

  private final CashbackRecordMapper cashbackRecordMapper;
  private final WechatPayService wechatPayService;
  private final WithdrawalWebSocketService withdrawalWebSocketService;
  private final UserWebSocketService userWebSocketService;

  public CashbackService(CashbackRecordMapper cashbackRecordMapper,
                         WechatPayService wechatPayService,
                         WithdrawalWebSocketService withdrawalWebSocketService,
                         UserWebSocketService userWebSocketService) {
    this.cashbackRecordMapper = cashbackRecordMapper;
    this.wechatPayService = wechatPayService;
    this.withdrawalWebSocketService = withdrawalWebSocketService;
    this.userWebSocketService = userWebSocketService;
  }

  public void grantPersonalCashback(Long userId, Long orderId, int paidOrderSeq, BigDecimal orderAmount) {
    BigDecimal amount = calculatePersonalCashbackAmount(paidOrderSeq, orderAmount);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }

    CashbackRecord record = new CashbackRecord();
    record.setUserId(userId);
    record.setOrderId(orderId);
    record.setType("PERSONAL_ORDER");
    record.setAmount(amount);
    record.setStatus("PENDING");
    record.setRemark("个人第" + paidOrderSeq + "单返现");
    record.setEligibleAt(LocalDateTime.now().plusDays(WITHDRAWAL_WAIT_DAYS));
    cashbackRecordMapper.insert(record);
  }

  public BigDecimal calculatePersonalCashbackAmount(int paidOrderSeq, BigDecimal orderAmount) {
    return switch (paidOrderSeq) {
      case 2 -> ratio(orderAmount, PERSONAL_SECOND_RATIO);
      case 3 -> ratio(orderAmount, PERSONAL_THIRD_RATIO);
      case 4 -> ratio(orderAmount, PERSONAL_FOURTH_RATIO);
      default -> BigDecimal.ZERO;
    };
  }

  public void grantInviteBatchCashback(Long inviterId, int firstPaidCount, BigDecimal firstOrderAmount) {
    grantInviteBatchCashback(inviterId, firstPaidCount, firstOrderAmount, null);
  }

  public void grantInviteFirstOrderCashback(Long inviterId, Long relatedOrderId, BigDecimal firstOrderAmount) {
    if (inviterId == null || relatedOrderId == null) {
      return;
    }

    List<CashbackRecord> orderRelated = cashbackRecordMapper.findByOrderId(relatedOrderId);
    boolean exists = orderRelated.stream().anyMatch(record ->
        "DOWNLINE_FIRST_ORDER".equals(record.getType())
            && inviterId.equals(record.getUserId())
            && !"CANCELLED".equals(normalizeStatus(record))
    );
    if (exists) {
      return;
    }

    BigDecimal amount = ratio(firstOrderAmount, INVITE_FIRST_ORDER_RATIO);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }

    CashbackRecord record = new CashbackRecord();
    record.setUserId(inviterId);
    record.setOrderId(relatedOrderId);
    record.setType("DOWNLINE_FIRST_ORDER");
    record.setAmount(amount);
    record.setStatus("PENDING");
    record.setRemark("邀请用户首单返现");
    record.setEligibleAt(LocalDateTime.now().plusDays(WITHDRAWAL_WAIT_DAYS));
    cashbackRecordMapper.insert(record);
  }

  public void grantInviteBatchCashback(Long inviterId, int firstPaidCount, BigDecimal firstOrderAmount, Long relatedOrderId) {
    if (firstPaidCount % 3 != 0) {
      return;
    }

    int batchNo = firstPaidCount / 3;
    if (cashbackRecordMapper.existsInviteBatch(inviterId, batchNo) > 0) {
      return;
    }

    BigDecimal ratio = batchNo == 1 ? INVITE_FIRST_BATCH_RATIO : INVITE_REPEAT_RATIO;
    BigDecimal amount = ratio(firstOrderAmount, ratio);
    CashbackRecord record = new CashbackRecord();
    record.setUserId(inviterId);
    record.setOrderId(relatedOrderId);
    record.setType("INVITE_BATCH");
    record.setAmount(amount);
    record.setBatchNo(batchNo);
    record.setStatus("PENDING");
    record.setRemark("邀请返现第" + batchNo + "批");
    record.setEligibleAt(LocalDateTime.now().plusDays(WITHDRAWAL_WAIT_DAYS));
    cashbackRecordMapper.insert(record);
  }

  public List<CashbackRecord> listByUser(Long userId) {
    return cashbackRecordMapper.findByUserId(userId);
  }

  public List<CashbackRecord> listByOrder(Long orderId) {
    return cashbackRecordMapper.findByOrderId(orderId);
  }

  public List<CashbackRecord> listWithdrawableByUser(Long userId) {
    return cashbackRecordMapper.findWithdrawableByUserId(userId);
  }

  public List<CashbackRecord> listInviteBatchByUser(Long userId) {
    return cashbackRecordMapper.findInviteBatchByUserId(userId);
  }

  public List<CashbackRecord> listAll() {
    return cashbackRecordMapper.findAll();
  }

  public Map<String, Object> buildUserSummary(Long userId) {
    List<CashbackRecord> records = listByUser(userId);
    LocalDateTime now = LocalDateTime.now();

    BigDecimal settlingTotal = BigDecimal.ZERO;
    BigDecimal maturedTotal = BigDecimal.ZERO;
    BigDecimal immatureTotal = BigDecimal.ZERO;
    BigDecimal requestableMaturedTotal = BigDecimal.ZERO;
    BigDecimal requestableImmatureTotal = BigDecimal.ZERO;
    BigDecimal inRequestTotal = BigDecimal.ZERO;
    BigDecimal processingTotal = BigDecimal.ZERO;
    BigDecimal transferredTotal = BigDecimal.ZERO;
    BigDecimal cancelledTotal = BigDecimal.ZERO;
    BigDecimal failedTotal = BigDecimal.ZERO;

    for (CashbackRecord record : records) {
      BigDecimal amount = money(record.getAmount());
      String status = normalizeStatus(record);
      boolean eligibleNow = isEligibleNow(record, now);
      boolean requested = record.getWithdrawalRequestId() != null;

      switch (status) {
        case "PENDING" -> {
          settlingTotal = settlingTotal.add(amount);
          if (eligibleNow) {
            maturedTotal = maturedTotal.add(amount);
            if (!requested) {
              requestableMaturedTotal = requestableMaturedTotal.add(amount);
            }
          } else {
            immatureTotal = immatureTotal.add(amount);
            if (!requested) {
              requestableImmatureTotal = requestableImmatureTotal.add(amount);
            }
          }
          if (requested) {
            inRequestTotal = inRequestTotal.add(amount);
          }
        }
        case "WAIT_USER_CONFIRM", "PROCESSING", "TRANSFERING", "CANCELING" ->
            processingTotal = processingTotal.add(amount);
        case "TRANSFERRED" -> transferredTotal = transferredTotal.add(amount);
        case "CANCELLED" -> cancelledTotal = cancelledTotal.add(amount);
        case "FAILED" -> failedTotal = failedTotal.add(amount);
        default -> {
        }
      }
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("settlingTotal", money(settlingTotal));
    payload.put("maturedTotal", money(maturedTotal));
    payload.put("immatureTotal", money(immatureTotal));
    payload.put("inRequestTotal", money(inRequestTotal));
    payload.put("transferredTotal", money(transferredTotal));
    payload.put("cancelledTotal", money(cancelledTotal));
    payload.put("requestableMaturedTotal", money(requestableMaturedTotal));
    payload.put("requestableImmatureTotal", money(requestableImmatureTotal));
    payload.put("processingTotal", money(processingTotal));
    payload.put("failedTotal", money(failedTotal));
    payload.put("readyTotal", money(maturedTotal));
    payload.put("requestableReadyTotal", money(requestableMaturedTotal));
    payload.put("requestedTotal", money(inRequestTotal));
    payload.put("arrivedTotal", money(transferredTotal));
    payload.put("validTotal", money(settlingTotal.add(processingTotal).add(transferredTotal).add(failedTotal)));
    return payload;
  }

  public Map<String, Object> buildMerchantTransferConfirmParamsForUser(Long cashbackId, Long userId) {
    CashbackRecord record = requireOwnedCashback(cashbackId, userId);
    String status = normalizeStatus(record);
    if ("TRANSFERRED".equals(status)) {
      throw new BusinessException("This cashback has already been transferred");
    }
    if (!"WAIT_USER_CONFIRM".equals(status)) {
      throw new BusinessException("This cashback is not waiting for user confirmation");
    }
    if (record.getTransferPackageInfo() == null || record.getTransferPackageInfo().isBlank()) {
      throw new BusinessException("Merchant transfer confirmation parameters are missing");
    }
    return wechatPayService.buildMerchantTransferConfirmParams(record);
  }

  public CashbackRecord syncTransferForUser(Long cashbackId, Long userId) {
    requireOwnedCashback(cashbackId, userId);
    return syncTransfer(cashbackId);
  }

  public CashbackRecord syncTransfer(Long cashbackId) {
    CashbackRecord record = cashbackRecordMapper.findById(cashbackId);
    if (record == null) {
      throw new BusinessException("Cashback record not found");
    }
    if (record.getOutDetailNo() == null || record.getOutDetailNo().isBlank()) {
      throw new BusinessException("This cashback has not created a WeChat merchant transfer bill");
    }

    try {
      WechatPayService.MerchantTransferBill detail =
          wechatPayService.queryTransferDetailByOutNo(record.getOutBatchNo(), record.getOutDetailNo());
      updateTransferResult(record, detail);
      return cashbackRecordMapper.findById(cashbackId);
    } catch (BusinessException ex) {
      if (ex.getMessage() != null && ex.getMessage().contains("NOT_FOUND") && "FAILED".equals(normalizeStatus(record))) {
        cashbackRecordMapper.resetFailedTransferForRetry(cashbackId);
        return cashbackRecordMapper.findById(cashbackId);
      }
      throw ex;
    }
  }

  public CashbackRecord updateTransferFromWechatNotification(Map<?, ?> payload) {
    String outBillNo = firstText(payload, "out_bill_no", "out_detail_no", "outBillNo");
    if (outBillNo == null) {
      throw new BusinessException("WeChat transfer notification missing out_bill_no");
    }

    CashbackRecord record = cashbackRecordMapper.findByOutDetailNo(outBillNo);
    if (record == null) {
      throw new BusinessException("Cashback transfer record not found for out_bill_no=" + outBillNo);
    }

    WechatPayService.MerchantTransferBill bill = new WechatPayService.MerchantTransferBill(
        outBillNo,
        firstText(payload, "transfer_bill_no", "transfer_id", "transfer_detail_id", "transferBillNo"),
        firstText(payload, "state", "status", "transfer_state", "transferState"),
        firstText(payload, "package_info", "packageInfo"),
        firstText(payload, "fail_reason", "fail_reason_type", "failReason"),
        firstText(payload, "create_time", "createTime"),
        firstText(payload, "update_time", "updateTime")
    );
    updateTransferResult(record, bill);
    return cashbackRecordMapper.findById(record.getId());
  }

  public double totalCashbackAmount() {
    Double value = cashbackRecordMapper.totalAmount();
    return value == null ? 0D : value;
  }

  public Map<String, Object> buildRules(Long productId, String productName, BigDecimal unitPrice) {
    BigDecimal normalizedUnitPrice = money(unitPrice);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("productId", productId);
    payload.put("productName", productName);
    payload.put("unitPrice", normalizedUnitPrice);
    payload.put("personalRules", buildPersonalRules(normalizedUnitPrice));
    payload.put("inviteRules", buildInviteRules(normalizedUnitPrice));
    payload.put("settleCondition", "订单已付款后结算");
    payload.put("inviteCondition", "同一被邀请人仅统计首单，按首单实付金额100%返现");
    return payload;
  }

  public boolean isTransferLocked(CashbackRecord record) {
    String status = normalizeStatus(record);
    return "PROCESSING".equals(status)
        || "WAIT_USER_CONFIRM".equals(status)
        || "TRANSFERING".equals(status)
        || "CANCELING".equals(status)
        || "TRANSFERRED".equals(status);
  }

  public void cancelForRefund(CashbackRecord record, String reason) {
    if (record == null || record.getId() == null) {
      return;
    }

    String status = normalizeStatus(record);
    if ("CANCELLED".equals(status)) {
      return;
    }
    if (isTransferLocked(record)) {
      throw new BusinessException("7天内退款需取消返现，但关联返现已发起打款或已到账，请先人工处理返现后再退款");
    }

    String remark = appendRemark(record.getRemark(), reason);
    cashbackRecordMapper.updateStatusAndRemark(record.getId(), "CANCELLED", remark);
    record.setStatus("CANCELLED");
    record.setRemark(remark);
    pushCashbackStatusChange(record);
  }

  private BigDecimal ratio(BigDecimal baseAmount, BigDecimal ratio) {
    if (baseAmount == null) {
      return BigDecimal.ZERO;
    }
    return baseAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
  }

  private List<Map<String, Object>> buildPersonalRules(BigDecimal unitPrice) {
    List<Map<String, Object>> rules = new ArrayList<>();
    rules.add(ruleItem("第1单", "不返", BigDecimal.ZERO, BigDecimal.ZERO));
    rules.add(ruleItem("第2单", "10%", PERSONAL_SECOND_RATIO, ratio(unitPrice, PERSONAL_SECOND_RATIO)));
    rules.add(ruleItem("第3单", "20%", PERSONAL_THIRD_RATIO, ratio(unitPrice, PERSONAL_THIRD_RATIO)));
    rules.add(ruleItem("第4单", "100%", PERSONAL_FOURTH_RATIO, ratio(unitPrice, PERSONAL_FOURTH_RATIO)));
    rules.add(ruleItem("第5单及以后", "不返", BigDecimal.ZERO, BigDecimal.ZERO));
    return rules;
  }

  private List<Map<String, Object>> buildInviteRules(BigDecimal unitPrice) {
    List<Map<String, Object>> rules = new ArrayList<>();
    rules.add(inviteRuleItem("每邀请1人", "被邀请人首单", "100%", INVITE_FIRST_ORDER_RATIO, ratio(unitPrice, INVITE_FIRST_ORDER_RATIO)));
    return rules;
  }

  private Map<String, Object> ruleItem(String label, String ratioText, BigDecimal ratio, BigDecimal cashbackAmount) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("label", label);
    item.put("ratioText", ratioText);
    item.put("ratio", ratio);
    item.put("cashbackAmount", cashbackAmount);
    return item;
  }

  private Map<String, Object> inviteRuleItem(
      String label,
      String peopleRule,
      String ratioText,
      BigDecimal ratio,
      BigDecimal cashbackAmount
  ) {
    Map<String, Object> item = ruleItem(label, ratioText, ratio, cashbackAmount);
    item.put("peopleRule", peopleRule);
    return item;
  }

  private BigDecimal money(BigDecimal amount) {
    if (amount == null) {
      return BigDecimal.ZERO;
    }
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  private boolean isEligibleNow(CashbackRecord record, LocalDateTime now) {
    return record == null || record.getEligibleAt() == null || !record.getEligibleAt().isAfter(now);
  }

  private String normalizeStatus(CashbackRecord record) {
    if (record == null || record.getStatus() == null) {
      return "";
    }
    return record.getStatus().trim().toUpperCase(Locale.ROOT);
  }

  private CashbackRecord requireOwnedCashback(Long cashbackId, Long userId) {
    CashbackRecord record = cashbackRecordMapper.findById(cashbackId);
    if (record == null) {
      throw new BusinessException("Cashback record not found");
    }
    if (userId == null || !userId.equals(record.getUserId())) {
      throw new BusinessException("No permission to operate this cashback");
    }
    return record;
  }

  private void updateTransferResult(CashbackRecord record, WechatPayService.MerchantTransferBill detail) {
    String nextStatus = mapMerchantTransferState(detail.getState());
    String failReason = isFailedMerchantTransferStatus(nextStatus)
        ? fallback(detail.getFailReason(), "WeChat merchant transfer failed: " + fallback(detail.getState(), "UNKNOWN"))
        : null;
    cashbackRecordMapper.updateTransferResult(
        record.getId(),
        nextStatus,
        fallback(record.getOutBatchNo(), null),
        fallback(detail.getOutBillNo(), record.getOutDetailNo()),
        fallback(detail.getTransferBillNo(), record.getTransferId()),
        fallback(detail.getTransferBillNo(), record.getTransferDetailId()),
        trimReason(failReason),
        trimPackageInfo(fallback(detail.getPackageInfo(), record.getTransferPackageInfo())),
        LocalDateTime.now()
    );
    record.setStatus(nextStatus);
    pushCashbackStatusChange(record);
  }

  private String mapMerchantTransferState(String detailStatus) {
    String status = detailStatus == null ? "" : detailStatus.trim().toUpperCase(Locale.ROOT);
    if ("SUCCESS".equals(status) || "SUCCEEDED".equals(status) || "TRANSFER_SUCCESS".equals(status)) {
      return "TRANSFERRED";
    }
    if ("FAIL".equals(status) || "FAILED".equals(status)) {
      return "FAILED";
    }
    if ("CANCELLED".equals(status)) {
      return "CANCELLED";
    }
    if ("WAIT_USER_CONFIRM".equals(status) || "TRANSFERING".equals(status) || "CANCELING".equals(status)) {
      return status;
    }
    return "PROCESSING";
  }

  private boolean isFailedMerchantTransferStatus(String status) {
    return "FAILED".equals(status) || "CANCELLED".equals(status);
  }

  private String firstText(Map<?, ?> payload, String... keys) {
    if (payload == null) {
      return null;
    }
    for (String key : keys) {
      Object value = payload.get(key);
      if (value == null) {
        continue;
      }
      String text = String.valueOf(value).trim();
      if (!text.isEmpty()) {
        return text;
      }
    }
    return null;
  }

  private String fallback(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String trimReason(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
  }

  private String trimPackageInfo(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() > 1000 ? trimmed.substring(0, 1000) : trimmed;
  }

  private String appendRemark(String current, String suffix) {
    if (suffix == null || suffix.isBlank()) {
      return current;
    }
    if (current == null || current.isBlank()) {
      return suffix.trim();
    }
    if (current.contains(suffix)) {
      return current;
    }
    return current + " | " + suffix.trim();
  }

  private void pushCashbackStatusChange(CashbackRecord record) {
    if (record == null || record.getId() == null) {
      return;
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", record.getId());
    data.put("status", record.getStatus());
    data.put("amount", record.getAmount());
    data.put("type", record.getType());
    data.put("orderId", record.getOrderId());
    // Push to the specific user
    userWebSocketService.pushToUser(record.getUserId(), "cashback-status-changed", data);
    // Also notify admin
    withdrawalWebSocketService.pushCashbackStatusUpdate(record.getId(), record.getStatus(), record.getUserId());
  }
}
