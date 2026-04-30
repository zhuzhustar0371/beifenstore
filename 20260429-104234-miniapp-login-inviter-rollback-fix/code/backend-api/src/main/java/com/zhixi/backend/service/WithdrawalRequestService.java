package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.AdminPageResult;
import com.zhixi.backend.mapper.CashbackDebtMapper;
import com.zhixi.backend.mapper.CashbackRecordMapper;
import com.zhixi.backend.mapper.WithdrawalRequestMapper;
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
import java.util.List;
import java.util.UUID;

@Service
public class WithdrawalRequestService {
  private static final String MUTEX_PREFIX = "withdrawal:mutex:";
  private static final String RATE_PREFIX = "withdrawal:rate:";
  private static final String IDEM_PREFIX = "withdrawal:idem:";
  private static final long MUTEX_TTL_SECONDS = 30;
  private static final long RATE_TTL_SECONDS = 60;
  private static final long IDEM_TTL_SECONDS = 86400; // 24 hours
  private static final int MAX_PENDING_PER_USER = 3;

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
    return createUserRequest(userId, null);
  }

  @Transactional
  public WithdrawalRequest createUserRequest(Long userId, String idempotencyKey) {
    userService.getUser(userId);

    // 1. Idempotency check — if idempotencyKey provided, check Redis
    if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
      String idemKey = IDEM_PREFIX + idempotencyKey;
      String existing = redisTemplate.opsForValue().get(idemKey);
      if (existing != null) {
        throw new BusinessException("重复提交，请勿短时间内多次发起提现");
      }
    }

    // 2. Mutual exclusion — prevent concurrent withdrawal from same user
    String mutexKey = MUTEX_PREFIX + userId;
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(mutexKey, "1", Duration.ofSeconds(MUTEX_TTL_SECONDS));
    if (locked == null || !locked) {
      throw new BusinessException("您有一个提现申请正在处理中，请稍后再试");
    }

    try {
      // 3. Rate limiting — at most 1 request per 60 seconds
      String rateKey = RATE_PREFIX + userId;
      String rateExists = redisTemplate.opsForValue().get(rateKey);
      if (rateExists != null) {
        throw new BusinessException("操作过于频繁，请60秒后再试");
      }
      redisTemplate.opsForValue().set(rateKey, "1", Duration.ofSeconds(RATE_TTL_SECONDS));

      // 4. Limit pending withdrawals per user
      int pendingCount = withdrawalRequestMapper.countPendingByUser(userId);
      if (pendingCount >= MAX_PENDING_PER_USER) {
        throw new BusinessException("您已有" + pendingCount + "个待处理的提现申请，请等待处理完成后再发起");
      }

      List<CashbackRecord> records = cashbackRecordMapper.findWithdrawableByUserId(userId);
      if (records.isEmpty()) {
        throw new BusinessException("No withdrawable cashback");
      }

      LocalDateTime now = LocalDateTime.now();
      List<CashbackRecord> withdrawableRecords = new ArrayList<>();
      for (CashbackRecord record : records) {
        if (record.getEligibleAt() != null && record.getEligibleAt().isAfter(now)) {
          cashbackRecordMapper.markEarlyWithdrawal(record.getId());
        }
        withdrawableRecords.add(record);
      }

      BigDecimal grossAmount = sumAmount(withdrawableRecords);
      BigDecimal debtTotal = cashbackDebtMapper.sumPendingByUserId(userId);
      BigDecimal netAmount = grossAmount;
      String remark = "User withdrawal request";

      if (debtTotal.compareTo(BigDecimal.ZERO) > 0) {
        netAmount = grossAmount.subtract(debtTotal);
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
          throw new BusinessException("您的可用返现" + grossAmount.setScale(2, RoundingMode.HALF_UP)
              + "元不足以抵扣待还欠款" + debtTotal.setScale(2, RoundingMode.HALF_UP) + "元");
        }
        cashbackDebtMapper.markAllDeducted(userId);
        remark = "User withdrawal request | debt deducted " + debtTotal.setScale(2, RoundingMode.HALF_UP);
      }

      String requestNo = generateRequestNo(userId);
      WithdrawalRequest request = new WithdrawalRequest();
      request.setUserId(userId);
      request.setAmount(netAmount);
      request.setStatus("PENDING");
      request.setSource("USER");
      request.setRemark(remark);
      request.setIdempotencyKey(idempotencyKey);
      request.setRequestNo(requestNo);
      withdrawalRequestMapper.insert(request);

      // 5. Store idempotency key → request ID mapping (after insert to get the ID)
      if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
        String idemKey = IDEM_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(idemKey, String.valueOf(request.getId()), Duration.ofSeconds(IDEM_TTL_SECONDS));
      }

      BigDecimal confirmedAmount = BigDecimal.ZERO;
      for (CashbackRecord record : withdrawableRecords) {
        int changed = cashbackRecordMapper.markWithdrawalRequested(record.getId(), request.getId());
        if (changed <= 0) {
          continue;
        }
        WithdrawalRequestItem item = new WithdrawalRequestItem();
        item.setRequestId(request.getId());
        item.setCashbackId(record.getId());
        item.setAmount(record.getAmount());
        withdrawalRequestMapper.insertItem(item);
        confirmedAmount = confirmedAmount.add(record.getAmount() == null ? BigDecimal.ZERO : record.getAmount());
      }

      if (confirmedAmount.compareTo(BigDecimal.ZERO) <= 0) {
        withdrawalRequestMapper.updateStatus(request.getId(), "CANCELLED", "No withdrawable cashback after locking");
        throw new BusinessException("No withdrawable cashback");
      }

      if (confirmedAmount.compareTo(netAmount) != 0) {
        withdrawalRequestMapper.updateAmount(request.getId(), confirmedAmount);
      }

      WithdrawalRequest created = withdrawalRequestMapper.findById(request.getId());
      withdrawalEventService.publishCreated(created);
      return created;
    } finally {
      redisTemplate.delete(mutexKey);
    }
  }

  public List<WithdrawalRequest> listByUser(Long userId) {
    userService.getUser(userId);
    return withdrawalRequestMapper.findByUserId(userId);
  }

  public AdminPageResult<WithdrawalRequest> pageAdmin(String status, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
    long total = withdrawalRequestMapper.countByAdminQuery(normalizedStatus);
    List<WithdrawalRequest> records = withdrawalRequestMapper.findByAdminQuery(normalizedStatus, offset, safeSize);
    return new AdminPageResult<>(records, total, safePage, safeSize);
  }

  public List<WithdrawalRequestItem> listItems(Long requestId) {
    return withdrawalRequestMapper.findItemsByRequestId(requestId);
  }

  public SseEmitter subscribe() {
    return withdrawalEventService.subscribe();
  }

  private String generateRequestNo(Long userId) {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String uid = String.format("%06d", userId % 1000000);
    String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
    return "WX" + timestamp.substring(timestamp.length() - 8) + uid + random;
  }

  private BigDecimal sumAmount(List<CashbackRecord> records) {
    BigDecimal amount = BigDecimal.ZERO;
    for (CashbackRecord record : records) {
      if (record.getAmount() != null) {
        amount = amount.add(record.getAmount());
      }
    }
    return amount;
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
