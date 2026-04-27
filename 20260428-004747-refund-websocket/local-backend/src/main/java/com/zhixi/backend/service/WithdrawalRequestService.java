package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.AdminPageResult;
import com.zhixi.backend.mapper.CashbackRecordMapper;
import com.zhixi.backend.mapper.WithdrawalRequestMapper;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.WithdrawalRequest;
import com.zhixi.backend.model.WithdrawalRequestItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WithdrawalRequestService {
  private final CashbackRecordMapper cashbackRecordMapper;
  private final WithdrawalRequestMapper withdrawalRequestMapper;
  private final UserService userService;
  private final WithdrawalEventService withdrawalEventService;

  public WithdrawalRequestService(
      CashbackRecordMapper cashbackRecordMapper,
      WithdrawalRequestMapper withdrawalRequestMapper,
      UserService userService,
      WithdrawalEventService withdrawalEventService
  ) {
    this.cashbackRecordMapper = cashbackRecordMapper;
    this.withdrawalRequestMapper = withdrawalRequestMapper;
    this.userService = userService;
    this.withdrawalEventService = withdrawalEventService;
  }

  @Transactional
  public WithdrawalRequest createUserRequest(Long userId) {
    userService.getUser(userId);
    List<CashbackRecord> records = cashbackRecordMapper.findWithdrawableByUserId(userId);
    if (records.isEmpty()) {
      throw new BusinessException("No withdrawable cashback");
    }

    WithdrawalRequest request = new WithdrawalRequest();
    request.setUserId(userId);
    request.setAmount(sumAmount(records));
    request.setStatus("PENDING");
    request.setSource("USER");
    request.setRemark("User withdrawal request");
    withdrawalRequestMapper.insert(request);

    BigDecimal confirmedAmount = BigDecimal.ZERO;
    for (CashbackRecord record : records) {
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

    if (confirmedAmount.compareTo(request.getAmount()) != 0) {
      withdrawalRequestMapper.updateAmount(request.getId(), confirmedAmount);
    }

    WithdrawalRequest created = withdrawalRequestMapper.findById(request.getId());
    withdrawalEventService.publishCreated(created);
    return created;
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
