package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.mapper.CashbackRecordMapper;
import com.zhixi.backend.model.CashbackRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CashbackService {
  private static final BigDecimal PERSONAL_SECOND_RATIO = BigDecimal.valueOf(0.10);
  private static final BigDecimal PERSONAL_THIRD_RATIO = BigDecimal.valueOf(0.20);
  private static final BigDecimal PERSONAL_FOURTH_RATIO = BigDecimal.valueOf(0.70);
  private static final BigDecimal INVITE_FIRST_BATCH_RATIO = BigDecimal.ONE;
  private static final BigDecimal INVITE_REPEAT_RATIO = BigDecimal.valueOf(0.20);

  private final CashbackRecordMapper cashbackRecordMapper;

  public CashbackService(CashbackRecordMapper cashbackRecordMapper) {
    this.cashbackRecordMapper = cashbackRecordMapper;
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
    record.setType("INVITE_BATCH");
    record.setAmount(amount);
    record.setBatchNo(batchNo);
    record.setStatus("PENDING");
    record.setRemark("邀请返现第" + batchNo + "批");
    cashbackRecordMapper.insert(record);
  }

  public List<CashbackRecord> listByUser(Long userId) {
    return cashbackRecordMapper.findByUserId(userId);
  }

  public List<CashbackRecord> listByOrder(Long orderId) {
    return cashbackRecordMapper.findByOrderId(orderId);
  }

  public List<CashbackRecord> listInviteBatchByUser(Long userId) {
    return cashbackRecordMapper.findInviteBatchByUserId(userId);
  }

  public List<CashbackRecord> listAll() {
    return cashbackRecordMapper.findAll();
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
    payload.put("inviteCondition", "同一被邀请人仅统计首单，不重复计入多批");
    return payload;
  }

  public boolean isTransferLocked(CashbackRecord record) {
    String status = normalizeStatus(record);
    return "PROCESSING".equals(status) || "TRANSFERRED".equals(status);
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
    rules.add(ruleItem("第4单", "70%", PERSONAL_FOURTH_RATIO, ratio(unitPrice, PERSONAL_FOURTH_RATIO)));
    rules.add(ruleItem("第5单及以后", "不返", BigDecimal.ZERO, BigDecimal.ZERO));
    return rules;
  }

  private List<Map<String, Object>> buildInviteRules(BigDecimal unitPrice) {
    List<Map<String, Object>> rules = new ArrayList<>();
    rules.add(inviteRuleItem("第1批", "满3人", "100%", INVITE_FIRST_BATCH_RATIO, ratio(unitPrice, INVITE_FIRST_BATCH_RATIO)));
    rules.add(inviteRuleItem("第2批", "满3人", "20%", INVITE_REPEAT_RATIO, ratio(unitPrice, INVITE_REPEAT_RATIO)));
    rules.add(inviteRuleItem("第3批及以后", "每满3人", "20%", INVITE_REPEAT_RATIO, ratio(unitPrice, INVITE_REPEAT_RATIO)));
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

  private String normalizeStatus(CashbackRecord record) {
    if (record == null || record.getStatus() == null) {
      return "";
    }
    return record.getStatus().trim();
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
}
