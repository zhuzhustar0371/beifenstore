package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.AdminPageResult;
import com.zhixi.backend.mapper.CashbackDebtMapper;
import com.zhixi.backend.mapper.CashbackRecordMapper;
import com.zhixi.backend.mapper.WithdrawalRequestMapper;
import com.zhixi.backend.model.CashbackDebt;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.WithdrawalRequest;
import com.zhixi.backend.model.WithdrawalRequestItem;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WithdrawalRequestService {
  private static final String MUTEX_PREFIX = "withdrawal:mutex:";
  private static final String RATE_PREFIX = "withdrawal:rate:";
  private static final String IDEM_PREFIX = "withdrawal:idem:";
  private static final long MUTEX_TTL_SECONDS = 30;
  private static final long RATE_TTL_SECONDS = 10;
  private static final long IDEM_TTL_SECONDS = 86400;
  private static final int MAX_PENDING_PER_USER = 3;
  private static final String APPLY_MODE_COMBINED = "COMBINED";
  private static final String APPLY_MODE_MATURED_ONLY = "MATURED_ONLY";
  private static final String APPLY_MODE_IMMATURE_ONLY = "IMMATURE_ONLY";

  private final CashbackRecordMapper cashbackRecordMapper;
  private final WithdrawalRequestMapper withdrawalRequestMapper;
  private final CashbackDebtMapper cashbackDebtMapper;
  private final UserService userService;
  private final WithdrawalEventService withdrawalEventService;
  private final StringRedisTemplate redisTemplate;

  public WithdrawalRequestService(
      CashbackRecordMapper cashbackRecordMapper,
      WithdrawalRequestMapper withdrawalRequestMapper,
      CashbackDebtMapper cashbackDebtMapper,
      UserService userService,
      WithdrawalEventService withdrawalEventService,
      StringRedisTemplate redisTemplate
  ) {
    this.cashbackRecordMapper = cashbackRecordMapper;
    this.withdrawalRequestMapper = withdrawalRequestMapper;
    this.cashbackDebtMapper = cashbackDebtMapper;
    this.userService = userService;
    this.withdrawalEventService = withdrawalEventService;
    this.redisTemplate = redisTemplate;
  }

  public WithdrawalRequest createUserRequest(Long userId) {
    return createUserRequest(userId, null, null);
  }

  public WithdrawalRequest createUserRequest(Long userId, String idempotencyKey) {
    return createUserRequest(userId, idempotencyKey, null);
  }

  @Transactional
  public WithdrawalRequest createUserRequest(Long userId, String idempotencyKey, String applyMode) {
    userService.getUser(userId);
    String normalizedApplyMode = normalizeApplyMode(applyMode);

    if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
      String idemKey = IDEM_PREFIX + idempotencyKey;
      String existing = redisTemplate.opsForValue().get(idemKey);
      if (existing != null) {
        throw new BusinessException("请勿重复提交提现申请");
      }
    }

    String mutexKey = MUTEX_PREFIX + userId;
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(mutexKey, "1", Duration.ofSeconds(MUTEX_TTL_SECONDS));
    if (locked == null || !locked) {
      throw new BusinessException("提现申请正在处理中，请稍候");
    }

    try {
      String rateKey = RATE_PREFIX + userId;
      String rateExists = redisTemplate.opsForValue().get(rateKey);
      if (rateExists != null) {
        throw new BusinessException("操作过于频繁，请稍后再试");
      }
      int pendingCount = withdrawalRequestMapper.countPendingByUser(userId);
      if (pendingCount >= MAX_PENDING_PER_USER) {
        throw new BusinessException("待处理的提现申请过多，请等待处理完成");
      }

      LocalDateTime now = LocalDateTime.now();
      List<CashbackRecord> selectedRecords = selectRecordsForApplyMode(
          cashbackRecordMapper.findPendingUnrequestedByUserId(userId),
          normalizedApplyMode,
          now
      );
      if (selectedRecords.isEmpty()) {
        throw new BusinessException("暂无可提现的返现记录");
      }

      BigDecimal readyAmount = BigDecimal.ZERO;
      BigDecimal pendingAmount = BigDecimal.ZERO;
      for (CashbackRecord record : selectedRecords) {
        BigDecimal amount = money(record.getAmount());
        if (isEligibleNow(record, now)) {
          readyAmount = readyAmount.add(amount);
        } else {
          pendingAmount = pendingAmount.add(amount);
          cashbackRecordMapper.markEarlyWithdrawal(record.getId());
        }
      }

      BigDecimal grossAmount = sumAmount(selectedRecords);
      BigDecimal debtTotal = cashbackDebtMapper.sumPendingByUserId(userId);
      BigDecimal netAmount = grossAmount;
      String remark = "User withdrawal request | mode " + normalizedApplyMode
          + " | requested " + money(grossAmount)
          + " | ready " + money(readyAmount)
          + " | pending " + money(pendingAmount);

      if (debtTotal.compareTo(BigDecimal.ZERO) > 0) {
        netAmount = grossAmount.subtract(debtTotal);
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
          throw new BusinessException("返现金额不足以抵扣待扣款项");
        }
        cashbackDebtMapper.markAllDeducted(userId);
        remark = remark + " | debt deducted " + debtTotal.setScale(2, RoundingMode.HALF_UP);
      }

      String requestNo = generateRequestNo(userId);
      WithdrawalRequest request = new WithdrawalRequest();
      request.setUserId(userId);
      request.setAmount(netAmount);
      request.setRequestedAmount(grossAmount);
      request.setStatus(readyAmount.compareTo(BigDecimal.ZERO) > 0 ? "PENDING" : "WAITING_MATURITY");
      request.setSource("USER");
      request.setApplyMode(normalizedApplyMode);
      request.setRemark(remark);
      request.setIdempotencyKey(idempotencyKey);
      request.setRequestNo(requestNo);
      withdrawalRequestMapper.insert(request);

      if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
        String idemKey = IDEM_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(idemKey, String.valueOf(request.getId()), Duration.ofSeconds(IDEM_TTL_SECONDS));
      }

      BigDecimal confirmedAmount = BigDecimal.ZERO;
      for (CashbackRecord record : selectedRecords) {
        int changed = cashbackRecordMapper.markWithdrawalRequested(record.getId(), request.getId());
        if (changed <= 0) {
          continue;
        }
        WithdrawalRequestItem item = new WithdrawalRequestItem();
        item.setRequestId(request.getId());
        item.setCashbackId(record.getId());
        item.setAmount(money(record.getAmount()));
        withdrawalRequestMapper.insertItem(item);
        confirmedAmount = confirmedAmount.add(money(record.getAmount()));
      }

      if (confirmedAmount.compareTo(BigDecimal.ZERO) <= 0) {
        withdrawalRequestMapper.updateStatus(request.getId(), "CANCELLED", "No cashback records after locking");
        throw new BusinessException("提现记录已被其他请求占用，请重试");
      }

      if (confirmedAmount.compareTo(netAmount) != 0) {
        withdrawalRequestMapper.updateRequestSnapshot(request.getId(), confirmedAmount, confirmedAmount, remark);
      }

      redisTemplate.opsForValue().set(rateKey, "1", Duration.ofSeconds(RATE_TTL_SECONDS));

      WithdrawalRequest created = enrichRequest(withdrawalRequestMapper.findById(request.getId()));
      withdrawalEventService.publishCreated(created);
      return created;
    } finally {
      redisTemplate.delete(mutexKey);
    }
  }

  public List<WithdrawalRequest> listByUser(Long userId) {
    userService.getUser(userId);
    return enrichRequests(withdrawalRequestMapper.findByUserId(userId));
  }

  public Map<String, Object> buildUserPreview(Long userId) {
    userService.getUser(userId);
    LocalDateTime now = LocalDateTime.now();
    List<CashbackRecord> records = cashbackRecordMapper.findPendingUnrequestedByUserId(userId);
    List<CashbackDebt> pendingDebts = cashbackDebtMapper.findPendingByUserId(userId);
    BigDecimal debtTotal = sumDebtAmount(pendingDebts);

    Map<String, Object> modePreviewMap = new LinkedHashMap<>();
    modePreviewMap.put(APPLY_MODE_COMBINED, buildModePreview(records, APPLY_MODE_COMBINED, debtTotal, now));
    modePreviewMap.put(APPLY_MODE_MATURED_ONLY, buildModePreview(records, APPLY_MODE_MATURED_ONLY, debtTotal, now));
    modePreviewMap.put(APPLY_MODE_IMMATURE_ONLY, buildModePreview(records, APPLY_MODE_IMMATURE_ONLY, debtTotal, now));

    String recommendedApplyMode = resolveRecommendedApplyMode(modePreviewMap);
    Map<String, Object> recommendedPreview = castPreview(modePreviewMap.get(recommendedApplyMode));

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("hasPendingDebt", debtTotal.compareTo(BigDecimal.ZERO) > 0);
    payload.put("pendingDebtTotal", money(debtTotal));
    payload.put("pendingDebtCount", pendingDebts.size());
    payload.put("pendingDebts", pendingDebts);
    payload.put("withdrawPreviewByMode", modePreviewMap);
    payload.put("recommendedApplyMode", recommendedApplyMode);
    payload.put("recommendedNetAmount", recommendedPreview.get("netAmount"));
    payload.put("recommendedGrossAmount", recommendedPreview.get("grossAmount"));
    return payload;
  }

  public AdminPageResult<WithdrawalRequest> pageAdmin(String status, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
    long total = withdrawalRequestMapper.countByAdminQuery(normalizedStatus);
    List<WithdrawalRequest> records = enrichRequests(withdrawalRequestMapper.findByAdminQuery(normalizedStatus, offset, safeSize));
    return new AdminPageResult<>(records, total, safePage, safeSize);
  }

  public List<WithdrawalRequestItem> listItems(Long requestId) {
    return withdrawalRequestMapper.findItemsByRequestId(requestId);
  }

  public SseEmitter subscribe() {
    return withdrawalEventService.subscribe();
  }

  private List<CashbackRecord> selectRecordsForApplyMode(
      List<CashbackRecord> records,
      String applyMode,
      LocalDateTime now
  ) {
    List<CashbackRecord> selected = new ArrayList<>();
    for (CashbackRecord record : records) {
      boolean eligibleNow = isEligibleNow(record, now);
      if (APPLY_MODE_MATURED_ONLY.equals(applyMode) && !eligibleNow) {
        continue;
      }
      if (APPLY_MODE_IMMATURE_ONLY.equals(applyMode) && eligibleNow) {
        continue;
      }
      selected.add(record);
    }
    selected.sort(this::compareCashbackForWithdrawal);
    return selected;
  }

  private Map<String, Object> buildModePreview(
      List<CashbackRecord> records,
      String applyMode,
      BigDecimal debtTotal,
      LocalDateTime now
  ) {
    List<CashbackRecord> selectedRecords = selectRecordsForApplyMode(records, applyMode, now);
    BigDecimal grossAmount = BigDecimal.ZERO;
    BigDecimal readyAmount = BigDecimal.ZERO;
    BigDecimal pendingAmount = BigDecimal.ZERO;

    for (CashbackRecord record : selectedRecords) {
      BigDecimal amount = money(record.getAmount());
      grossAmount = grossAmount.add(amount);
      if (isEligibleNow(record, now)) {
        readyAmount = readyAmount.add(amount);
      } else {
        pendingAmount = pendingAmount.add(amount);
      }
    }

    BigDecimal deductibleDebtAmount = grossAmount.min(debtTotal);
    BigDecimal netAmount = grossAmount.subtract(debtTotal);
    BigDecimal remainingDebtAmount = BigDecimal.ZERO;
    boolean canSubmit = false;
    String reason = null;

    if (selectedRecords.isEmpty()) {
      reason = "该模式暂无可申请返现金额";
      netAmount = BigDecimal.ZERO;
    } else if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
      reason = "返现金额不足以抵扣待扣款项";
      remainingDebtAmount = money(debtTotal.subtract(grossAmount));
      netAmount = BigDecimal.ZERO;
    } else {
      canSubmit = true;
      netAmount = money(netAmount);
    }

    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("mode", applyMode);
    preview.put("label", applyModeLabel(applyMode));
    preview.put("recordCount", selectedRecords.size());
    preview.put("grossAmount", money(grossAmount));
    preview.put("readyAmount", money(readyAmount));
    preview.put("pendingAmount", money(pendingAmount));
    preview.put("debtAmount", money(debtTotal));
    preview.put("deductibleDebtAmount", money(deductibleDebtAmount));
    preview.put("remainingDebtAmount", remainingDebtAmount);
    preview.put("netAmount", netAmount);
    preview.put("canSubmit", canSubmit);
    preview.put("reason", reason);
    return preview;
  }

  private String normalizeApplyMode(String applyMode) {
    if (applyMode == null || applyMode.isBlank()) {
      return APPLY_MODE_MATURED_ONLY;
    }
    String mode = applyMode.trim().toUpperCase(Locale.ROOT);
    return switch (mode) {
      case "ALL", "MERGE", "MERGED", "COMBINED" -> APPLY_MODE_COMBINED;
      case "IMMATURE", "EARLY", "IMMATURE_ONLY", "EARLY_ONLY" -> APPLY_MODE_IMMATURE_ONLY;
      default -> APPLY_MODE_MATURED_ONLY;
    };
  }

  private String applyModeLabel(String applyMode) {
    return switch (applyMode) {
      case APPLY_MODE_COMBINED -> "合并申请";
      case APPLY_MODE_IMMATURE_ONLY -> "仅申请未满7天";
      default -> "仅申请已满7天";
    };
  }

  private String resolveRecommendedApplyMode(Map<String, Object> modePreviewMap) {
    String recommended = APPLY_MODE_COMBINED;
    BigDecimal bestNetAmount = BigDecimal.valueOf(-1);
    for (String mode : List.of(APPLY_MODE_COMBINED, APPLY_MODE_MATURED_ONLY, APPLY_MODE_IMMATURE_ONLY)) {
      Map<String, Object> preview = castPreview(modePreviewMap.get(mode));
      boolean canSubmit = Boolean.TRUE.equals(preview.get("canSubmit"));
      BigDecimal netAmount = money((BigDecimal) preview.get("netAmount"));
      if (canSubmit && netAmount.compareTo(bestNetAmount) > 0) {
        bestNetAmount = netAmount;
        recommended = mode;
      }
    }
    return recommended;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castPreview(Object value) {
    if (value instanceof Map<?, ?> preview) {
      return (Map<String, Object>) preview;
    }
    return new LinkedHashMap<>();
  }

  private WithdrawalRequest enrichRequest(WithdrawalRequest request) {
    if (request == null) {
      return null;
    }
    List<WithdrawalRequestItem> items = withdrawalRequestMapper.findItemsByRequestId(request.getId());
    BigDecimal requestedAmount = BigDecimal.ZERO;
    BigDecimal readyAmount = BigDecimal.ZERO;
    BigDecimal pendingAmount = BigDecimal.ZERO;
    LocalDateTime earliestEligibleAt = null;
    LocalDateTime now = LocalDateTime.now();

    for (WithdrawalRequestItem item : items) {
      CashbackRecord record = cashbackRecordMapper.findById(item.getCashbackId());
      if (record == null) {
        continue;
      }
      BigDecimal amount = money(item.getAmount() != null ? item.getAmount() : record.getAmount());
      requestedAmount = requestedAmount.add(amount);
      if (isEligibleNow(record, now)) {
        readyAmount = readyAmount.add(amount);
      } else {
        pendingAmount = pendingAmount.add(amount);
      }
      if (record.getEligibleAt() != null
          && (earliestEligibleAt == null || record.getEligibleAt().isBefore(earliestEligibleAt))) {
        earliestEligibleAt = record.getEligibleAt();
      }
    }

    if (request.getRequestedAmount() == null || request.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
      request.setRequestedAmount(money(requestedAmount));
    }
    request.setReadyAmount(money(readyAmount));
    request.setPendingAmount(money(pendingAmount));
    request.setSuggestedAmount(money(readyAmount));
    request.setEarliestEligibleAt(earliestEligibleAt);
    return request;
  }

  private List<WithdrawalRequest> enrichRequests(List<WithdrawalRequest> requests) {
    List<WithdrawalRequest> result = new ArrayList<>();
    for (WithdrawalRequest request : requests) {
      result.add(enrichRequest(request));
    }
    return result;
  }

  private String generateRequestNo(Long userId) {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String uid = String.format("%06d", userId % 1000000);
    String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
    return "WX" + timestamp.substring(timestamp.length() - 8) + uid + random;
  }

  private BigDecimal sumAmount(List<CashbackRecord> records) {
    BigDecimal amount = BigDecimal.ZERO;
    for (CashbackRecord record : records) {
      amount = amount.add(money(record.getAmount()));
    }
    return money(amount);
  }

  private BigDecimal sumDebtAmount(List<CashbackDebt> debts) {
    BigDecimal amount = BigDecimal.ZERO;
    for (CashbackDebt debt : debts) {
      amount = amount.add(money(debt.getAmount()));
    }
    return money(amount);
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

  private int compareCashbackForWithdrawal(CashbackRecord left, CashbackRecord right) {
    LocalDateTime leftTime = left.getEligibleAt() == null ? LocalDateTime.MIN : left.getEligibleAt();
    LocalDateTime rightTime = right.getEligibleAt() == null ? LocalDateTime.MIN : right.getEligibleAt();
    int timeCompare = leftTime.compareTo(rightTime);
    if (timeCompare != 0) {
      return timeCompare;
    }
    long leftId = left.getId() == null ? Long.MAX_VALUE : left.getId();
    long rightId = right.getId() == null ? Long.MAX_VALUE : right.getId();
    return Long.compare(leftId, rightId);
  }

  private int safePage(Integer page) {
    return (page == null || page < 1) ? 1 : page;
  }

  private int safeSize(Integer size) {
    if (size == null || size < 1) {
      return 20;
    }
    return Math.min(size, 100);
  }
}
