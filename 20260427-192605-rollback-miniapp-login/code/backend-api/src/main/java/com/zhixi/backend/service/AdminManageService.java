package com.zhixi.backend.service;

import com.wechat.pay.java.service.refund.model.Status;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.AdminPageResult;
import com.zhixi.backend.dto.AdminProductUpsertRequest;
import com.zhixi.backend.dto.AdminUserVO;
import com.zhixi.backend.mapper.CashbackRecordMapper;
import com.zhixi.backend.mapper.InviteRelationMapper;
import com.zhixi.backend.mapper.OrderMapper;
import com.zhixi.backend.mapper.ProductMapper;
import com.zhixi.backend.mapper.ShippingRecordMapper;
import com.zhixi.backend.mapper.UserMapper;
import com.zhixi.backend.mapper.UserWechatAuthMapper;
import com.zhixi.backend.mapper.WithdrawalRequestMapper;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.Order;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.UserWechatAuth;
import com.zhixi.backend.model.WithdrawalRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminManageService {
  private static final String WECHAT_SOURCE_MINIAPP = "MINIAPP";
  private static final String WECHAT_SOURCE_WEB = "WEB";
  private static final DateTimeFormatter ADMIN_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final UserMapper userMapper;
  private final ProductMapper productMapper;
  private final InviteRelationMapper inviteRelationMapper;
  private final OrderMapper orderMapper;
  private final CashbackRecordMapper cashbackRecordMapper;
  private final ShippingRecordMapper shippingRecordMapper;
  private final UserWechatAuthMapper userWechatAuthMapper;
  private final WithdrawalRequestMapper withdrawalRequestMapper;
  private final WechatPayService wechatPayService;
  private final WechatTradeManagementService wechatTradeManagementService;
  private final OrderService orderService;
  private final CashbackService cashbackService;
  private final JdbcTemplate jdbcTemplate;

  public AdminManageService(
      UserMapper userMapper,
      ProductMapper productMapper,
      InviteRelationMapper inviteRelationMapper,
      OrderMapper orderMapper,
      CashbackRecordMapper cashbackRecordMapper,
      ShippingRecordMapper shippingRecordMapper,
      UserWechatAuthMapper userWechatAuthMapper,
      WithdrawalRequestMapper withdrawalRequestMapper,
      WechatPayService wechatPayService,
      WechatTradeManagementService wechatTradeManagementService,
      OrderService orderService,
      CashbackService cashbackService,
      JdbcTemplate jdbcTemplate
  ) {
    this.userMapper = userMapper;
    this.productMapper = productMapper;
    this.inviteRelationMapper = inviteRelationMapper;
    this.orderMapper = orderMapper;
    this.cashbackRecordMapper = cashbackRecordMapper;
    this.shippingRecordMapper = shippingRecordMapper;
    this.userWechatAuthMapper = userWechatAuthMapper;
    this.withdrawalRequestMapper = withdrawalRequestMapper;
    this.wechatPayService = wechatPayService;
    this.wechatTradeManagementService = wechatTradeManagementService;
    this.orderService = orderService;
    this.cashbackService = cashbackService;
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<User> listUsers() {
    return userMapper.findAll();
  }

  public AdminPageResult<AdminUserVO> pageUsers(String keyword, Integer status, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = userMapper.countByAdminQuery(keyword, status);
    List<User> records = userMapper.findByAdminQuery(keyword, status, offset, safeSize);
    List<AdminUserVO> result = records.stream().map(this::toAdminUserVO).collect(Collectors.toList());
    return new AdminPageResult<>(result, total, safePage, safeSize);
  }

  public List<Product> listProducts() {
    return productMapper.findAll();
  }

  public AdminPageResult<Product> pageProducts(String keyword, Boolean active, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = productMapper.countByAdminQuery(keyword, active);
    List<Product> records = productMapper.findByAdminQuery(keyword, active, offset, safeSize);
    return new AdminPageResult<>(records, total, safePage, safeSize);
  }

  public List<InviteRelation> listInvites() {
    return inviteRelationMapper.findAll();
  }

  public AdminPageResult<InviteRelation> pageInvites(Long inviterId, Long inviteeId, Boolean firstPaidOnly, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = inviteRelationMapper.countByAdminQuery(inviterId, inviteeId, firstPaidOnly);
    List<InviteRelation> records = inviteRelationMapper.findByAdminQuery(inviterId, inviteeId, firstPaidOnly, offset, safeSize);
    return new AdminPageResult<>(records, total, safePage, safeSize);
  }

  public AdminPageResult<Order> pageOrders(String status, Long userId, String keyword, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = orderMapper.countByAdminQuery(status, userId, keyword);
    List<Order> records = orderMapper.findByAdminQuery(status, userId, keyword, offset, safeSize);
    return new AdminPageResult<>(records, total, safePage, safeSize);
  }

  public AdminPageResult<CashbackRecord> pageCashbacks(Long userId, String type, String status, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = cashbackRecordMapper.countByAdminQuery(userId, type, status);
    List<CashbackRecord> records = cashbackRecordMapper.findByAdminQuery(userId, type, status, offset, safeSize);
    return new AdminPageResult<>(records, total, safePage, safeSize);
  }

  @Transactional
  public void updateProductStatus(Long productId, Boolean active) {
    Product product = productMapper.findById(productId);
    if (product == null) {
      throw new BusinessException("商品不存在");
    }
    productMapper.updateActive(productId, active);
  }

  @Transactional
  public Product createProduct(AdminProductUpsertRequest request) {
    Product product = new Product();
    product.setName(request.getName().trim());
    product.setPrice(request.getPrice());
    product.setDescription(request.getDescription());
    product.setImageUrl(request.getImageUrl());
    product.setActive(request.getActive());
    productMapper.insert(product);
    return productMapper.findById(product.getId());
  }

  @Transactional
  public Product updateProduct(Long productId, AdminProductUpsertRequest request) {
    Product current = productMapper.findById(productId);
    if (current == null) {
      throw new BusinessException("商品不存在");
    }
    current.setName(request.getName().trim());
    current.setPrice(request.getPrice());
    current.setDescription(request.getDescription());
    current.setImageUrl(request.getImageUrl());
    current.setActive(request.getActive());
    productMapper.update(current);
    return productMapper.findById(productId);
  }

  @Transactional
  public void deleteProduct(Long productId) {
    Product current = productMapper.findById(productId);
    if (current == null) {
      throw new BusinessException("商品不存在");
    }
    productMapper.deleteById(productId);
  }

  @Transactional
  public void updateUserStatus(Long userId, Integer status) {
    User user = userMapper.findById(userId);
    if (user == null) {
      throw new BusinessException("用户不存在");
    }
    userMapper.updateStatus(userId, status);
  }

  @Transactional
  public void shipOrder(Long orderId, String trackingNo, String expressCompany) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BusinessException("订单不存在");
    }
    String normalizedTrackingNo = normalizeRequiredText(trackingNo, "物流单号不能为空");
    String normalizedExpressCompany = normalizeRequiredText(expressCompany, "物流公司编码不能为空").toUpperCase(Locale.ROOT);
    if (!"PAID".equals(order.getStatus())) {
      throw new BusinessException("当前订单状态不可发货");
    }
    if (isWechatPaidOrder(order)) {
      String payerOpenid = resolveWechatMiniappOpenid(order.getUserId());
      if (payerOpenid.isBlank()) {
        throw new BusinessException("订单缺少支付用户 openid，无法同步微信发货信息");
      }
      wechatTradeManagementService.uploadShippingInfo(
          order,
          payerOpenid,
          normalizedTrackingNo,
          normalizedExpressCompany,
          buildShippingItemDesc(order)
      );
    }
    int changed = orderMapper.markShipped(orderId, normalizedTrackingNo);
    if (changed <= 0) {
      throw new BusinessException("当前订单状态不可发货");
    }
    int updatedShipping = shippingRecordMapper.updateByOrderId(orderId, normalizedExpressCompany, normalizedTrackingNo);
    if (updatedShipping <= 0) {
      shippingRecordMapper.insert(orderId, normalizedExpressCompany, normalizedTrackingNo);
    }
  }

  @Transactional
  public void refundOrder(Long orderId, String reason) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BusinessException("订单不存在");
    }
    if (!"PAID".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
      throw new BusinessException("当前订单状态不可退款");
    }
    if ("PROCESSING".equals(order.getRefundStatus()) || "SUCCESS".equals(order.getRefundStatus())) {
      throw new BusinessException("当前订单退款已提交，请勿重复操作");
    }

    String normalizedReason = reason == null || reason.isBlank() ? "协商退款" : reason.trim();
    LocalDateTime refundApplyAt = LocalDateTime.now();
    BigDecimal cashbackDeduction = orderService.calculateRefundCashbackDeduction(order);
    BigDecimal refundAmount = calculateActualRefundAmount(order.getTotalAmount(), cashbackDeduction);
    String refundReason = appendCashbackDeductionReason(normalizedReason, cashbackDeduction, refundAmount);
    if ("WECHAT".equals(order.getPayType())) {
      if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
        WechatPayService.RefundApplyResult wxRefund = wechatPayService.refundOrder(order, refundReason, refundAmount);
        String refundStatus = mapRefundStatus(wxRefund.getStatus());
        orderMapper.updateRefundRequest(orderId, refundStatus, wxRefund.getOutRefundNo(), wxRefund.getRefundId(), refundApplyAt);
        if ("SUCCESS".equals(refundStatus)) {
          orderMapper.markRefunded(orderId);
          orderService.handleRefundSuccessByOrderId(orderId);
        }
      } else {
        orderMapper.updateRefundRequest(orderId, "SUCCESS", "CASHBACK_DEDUCTED", "CASHBACK_DEDUCTED", refundApplyAt);
        orderMapper.markRefunded(orderId);
        orderService.handleRefundSuccessByOrderId(orderId);
      }
    } else {
      orderMapper.updateRefundRequest(orderId, "SUCCESS", "MANUAL", "MANUAL", refundApplyAt);
      orderMapper.markRefunded(orderId);
      orderService.handleRefundSuccessByOrderId(orderId);
    }
  }

  @Transactional
  public WithdrawalRequest approveWithdrawalRequest(Long requestId) {
    WithdrawalRequest request = withdrawalRequestMapper.findById(requestId);
    if (request == null) {
      throw new BusinessException("Withdrawal request not found");
    }
    if (!"PENDING".equals(request.getStatus())) {
      throw new BusinessException("Withdrawal request is not pending");
    }

    List<CashbackRecord> records = cashbackRecordMapper.findByWithdrawalRequestId(requestId);
    if (records.isEmpty()) {
      withdrawalRequestMapper.updateStatus(requestId, "FAILED", "No cashback items");
      throw new BusinessException("Withdrawal request has no cashback items");
    }

    List<CashbackRecord> transferableRecords = resolveRequestCashbacksForApproval(records);
    withdrawalRequestMapper.updateStatus(requestId, "APPROVED", "Approved by admin");
    try {
      for (CashbackRecord record : transferableRecords) {
        transferCashback(record.getId());
      }
      return withdrawalRequestMapper.findById(requestId);
    } catch (RuntimeException ex) {
      withdrawalRequestMapper.updateStatus(requestId, "FAILED", trimReason(ex.getMessage()));
      throw ex;
    }
  }

  public CashbackRecord transferCashback(Long cashbackId) {
    CashbackRecord cb = cashbackRecordMapper.findById(cashbackId);
    if (cb == null) {
      throw new BusinessException("返现记录不存在");
    }
    if (!"PENDING".equals(cb.getStatus()) && !isRetryableTransferConfigFailure(cb)) {
      throw new BusinessException("该笔佣金不处于待发状态");
    }
    ensureCashbackEligibleForTransfer(cb);
    User user = userMapper.findById(cb.getUserId());
    String openid = resolveCashbackOpenid(user);
    if (user == null || openid == null) {
      throw new BusinessException("该用户不存在或尚未绑定微信小程序，无法进行微信打款");
    }

    String outBatchNo = stableOutBatchNo(cb);
    String outDetailNo = stableOutDetailNo(cb);
    LocalDateTime now = LocalDateTime.now();
    int changed = cashbackRecordMapper.markTransferProcessing(cashbackId, cb.getStatus(), outBatchNo, outDetailNo, now);
    if (changed <= 0) {
      throw new BusinessException("返现状态已变化，请刷新后重试");
    }
    cb.setOutBatchNo(outBatchNo);
    cb.setOutDetailNo(outDetailNo);

    try {
      WechatPayService.MerchantTransferBill resp = wechatPayService.transferToWallet(cb, openid);
      String nextStatus = mapMerchantTransferState(resp.getState());
      String failReason = isFailedMerchantTransferStatus(nextStatus) ? fallback(resp.getFailReason(), "微信商家转账失败，状态：" + fallback(resp.getState(), "UNKNOWN")) : null;
      cashbackRecordMapper.updateTransferResult(
          cashbackId,
          nextStatus,
          cb.getOutBatchNo(),
          fallback(resp.getOutBillNo(), cb.getOutDetailNo()),
          resp.getTransferBillNo(),
          resp.getTransferBillNo(),
          trimReason(failReason),
          trimPackageInfo(resp.getPackageInfo()),
          LocalDateTime.now()
      );
      return cashbackRecordMapper.findById(cashbackId);
    } catch (BusinessException ex) {
      String failedStatus = ex.getMessage() != null && ex.getMessage().contains("网络异常") ? "PROCESSING" : "FAILED";
      cashbackRecordMapper.updateTransferResult(
          cashbackId,
          failedStatus,
          cb.getOutBatchNo(),
          cb.getOutDetailNo(),
          null,
          null,
          trimReason(ex.getMessage()),
          null,
          LocalDateTime.now()
      );
      throw ex;
    }
  }

  public CashbackRecord syncCashbackTransfer(Long cashbackId) {
    return cashbackService.syncTransfer(cashbackId);
  }

  private CashbackRecord syncCashbackTransferLegacy(Long cashbackId) {
    CashbackRecord cb = cashbackRecordMapper.findById(cashbackId);
    if (cb == null) {
      throw new BusinessException("返现记录不存在");
    }
    if (cb.getOutBatchNo() == null || cb.getOutBatchNo().isBlank()
        || cb.getOutDetailNo() == null || cb.getOutDetailNo().isBlank()) {
      throw new BusinessException("该返现尚未发起微信打款，无法同步状态");
    }

    try {
      WechatPayService.MerchantTransferBill detail = wechatPayService.queryTransferDetailByOutNo(cb.getOutBatchNo(), cb.getOutDetailNo());
      String nextStatus = mapMerchantTransferState(detail.getState());
      String failReason = isFailedMerchantTransferStatus(nextStatus) ? fallback(detail.getFailReason(), "微信商家转账失败，状态：" + fallback(detail.getState(), "UNKNOWN")) : null;
      cashbackRecordMapper.updateTransferResult(
          cashbackId,
          nextStatus,
          cb.getOutBatchNo(),
          fallback(detail.getOutBillNo(), cb.getOutDetailNo()),
          fallback(detail.getTransferBillNo(), cb.getTransferId()),
          fallback(detail.getTransferBillNo(), cb.getTransferDetailId()),
          trimReason(failReason),
          trimPackageInfo(fallback(detail.getPackageInfo(), cb.getTransferPackageInfo())),
          LocalDateTime.now()
      );
      return cashbackRecordMapper.findById(cashbackId);
    } catch (BusinessException ex) {
      if (ex.getMessage() != null && ex.getMessage().contains("NOT_FOUND") && "FAILED".equals(cb.getStatus())) {
        cashbackRecordMapper.resetFailedTransferForRetry(cashbackId);
        return cashbackRecordMapper.findById(cashbackId);
      }
      throw ex;
    }
  }

  public long todayUsers() {
    return userMapper.countToday();
  }

  public long todayOrders() {
    return orderMapper.countTodayOrders();
  }

  public long pendingShipments() {
    return orderMapper.countPendingShipments();
  }

  public double todayIncome() {
    Double value = orderMapper.todayPaidAmount();
    return value == null ? 0D : value;
  }

  public double todayCashback() {
    Double value = cashbackRecordMapper.todayAmount();
    return value == null ? 0D : value;
  }

  public Map<String, Object> resetAllCashbackStats() {
    int userUpdated = userMapper.resetAllCashbackStats();
    int cashbackCancelled = cashbackRecordMapper.cancelAllPending();
    int inviteCleared = inviteRelationMapper.clearAllFirstPaid();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("userUpdated", userUpdated);
    result.put("cashbackCancelled", cashbackCancelled);
    result.put("inviteCleared", inviteCleared);
    return result;
  }

  @Transactional
  public Map<String, Object> resetAllUsers() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("smsCodes", jdbcTemplate.update("DELETE FROM sms_login_codes"));
    result.put("addresses", jdbcTemplate.update("DELETE FROM user_addresses"));
    result.put("sessions", jdbcTemplate.update("DELETE FROM user_sessions"));
    result.put("wechatAuths", jdbcTemplate.update("DELETE FROM user_wechat_auth"));
    result.put("inviteRelations", jdbcTemplate.update("DELETE FROM invite_relations"));
    result.put("cashbacks", jdbcTemplate.update("DELETE FROM cashback_records"));
    result.put("orders", jdbcTemplate.update("DELETE FROM orders"));
    int usersDeleted = jdbcTemplate.update("DELETE FROM users");
    result.put("users", usersDeleted);
    return result;
  }

  private String resolveCashbackOpenid(User user) {
    if (user == null) {
      return null;
    }
    if (user.getMiniappOpenid() != null && !user.getMiniappOpenid().isBlank()) {
      return user.getMiniappOpenid();
    }
    UserWechatAuth auth = userWechatAuthMapper.findByUserIdAndSource(user.getId(), WECHAT_SOURCE_MINIAPP);
    if (auth == null || auth.getOpenid() == null || auth.getOpenid().isBlank()) {
      return null;
    }
    User owner = userMapper.findByMiniappOpenid(auth.getOpenid());
    if (owner != null && !owner.getId().equals(user.getId())) {
      return null;
    }
    userMapper.updateMiniappOpenid(user.getId(), auth.getOpenid());
    user.setMiniappOpenid(auth.getOpenid());
    return auth.getOpenid();
  }

  private String stableOutBatchNo(CashbackRecord cb) {
    if (cb.getOutBatchNo() != null && !cb.getOutBatchNo().isBlank()) {
      return cb.getOutBatchNo().trim();
    }
    return "ZXCB" + cb.getId();
  }

  private String stableOutDetailNo(CashbackRecord cb) {
    if (cb.getOutDetailNo() != null && !cb.getOutDetailNo().isBlank()) {
      return cb.getOutDetailNo().trim();
    }
    return "ZXCBD" + cb.getId();
  }

  private String mapMerchantTransferState(String detailStatus) {
    String status = normalizeStatus(detailStatus);
    if ("SUCCESS".equals(status) || "SUCCEEDED".equals(status)) {
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

  private String normalizeStatus(String status) {
    return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
  }

  private boolean isRetryableTransferConfigFailure(CashbackRecord cb) {
    if (cb == null || !"FAILED".equals(cb.getStatus())) {
      return false;
    }
    String reason = cb.getTransferFailReason();
    return (cb.getTransferId() == null || cb.getTransferId().isBlank())
        && reason != null
        && (
            reason.contains("NOT_ENOUGH")
                || reason.contains("NO_AUTH")
                || reason.contains("升级版本")
                || reason.contains("升级前功能")
                || reason.contains("INVALID_REQUEST")
                || reason.contains("尚未获取该转账场景")
                || reason.contains("转账场景")
                || reason.contains("transfer_scene_id")
        );
  }

  private List<CashbackRecord> resolveRequestCashbacksForApproval(List<CashbackRecord> records) {
    List<CashbackRecord> transferableRecords = new java.util.ArrayList<>();
    BigDecimal readyAmount = BigDecimal.ZERO;
    BigDecimal pendingAmount = BigDecimal.ZERO;
    LocalDateTime nextEligibleAt = null;

    for (CashbackRecord record : records) {
      if (record == null) {
        continue;
      }
      if (isCashbackTransferHandled(record)) {
        continue;
      }
      if (!"PENDING".equals(record.getStatus()) && !isRetryableTransferConfigFailure(record)) {
        throw new BusinessException("提现申请包含不可处理的返现记录 #" + record.getId() + "，当前状态：" + record.getStatus());
      }
      if (isEligibleForTransfer(record)) {
        readyAmount = readyAmount.add(safeAmount(record.getAmount()));
        transferableRecords.add(record);
        continue;
      }
      pendingAmount = pendingAmount.add(safeAmount(record.getAmount()));
      LocalDateTime eligibleAt = record.getEligibleAt();
      if (eligibleAt != null && (nextEligibleAt == null || eligibleAt.isBefore(nextEligibleAt))) {
        nextEligibleAt = eligibleAt;
      }
    }

    if (pendingAmount.compareTo(BigDecimal.ZERO) > 0) {
      throw new BusinessException(buildWithdrawalNotReadyMessage(readyAmount, pendingAmount, nextEligibleAt));
    }

    return transferableRecords;
  }

  private void ensureCashbackEligibleForTransfer(CashbackRecord cashback) {
    if (isEligibleForTransfer(cashback)) {
      return;
    }
    throw new BusinessException("该笔返现未满7天，最早可打款时间：" + formatDateTime(cashback.getEligibleAt()));
  }

  private boolean isEligibleForTransfer(CashbackRecord cashback) {
    return cashback == null
        || cashback.getEligibleAt() == null
        || !cashback.getEligibleAt().isAfter(LocalDateTime.now());
  }

  private boolean isCashbackTransferHandled(CashbackRecord cashback) {
    if (cashback == null) {
      return false;
    }
    String status = cashback.getStatus();
    return "WAIT_USER_CONFIRM".equals(status)
        || "PROCESSING".equals(status)
        || "TRANSFERING".equals(status)
        || "CANCELING".equals(status)
        || "TRANSFERRED".equals(status);
  }

  private BigDecimal safeAmount(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  private String buildWithdrawalNotReadyMessage(BigDecimal readyAmount, BigDecimal pendingAmount, LocalDateTime nextEligibleAt) {
    StringBuilder message = new StringBuilder("该提现申请包含未满7天的返现");
    message.append("，当前可打款 ¥").append(formatAmount(readyAmount));
    message.append("，待满7天 ¥").append(formatAmount(pendingAmount));
    if (nextEligibleAt != null) {
      message.append("，最早可打款时间：").append(formatDateTime(nextEligibleAt));
    }
    return message.toString();
  }

  private String formatAmount(BigDecimal amount) {
    return safeAmount(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private String formatDateTime(LocalDateTime time) {
    return time == null ? "-" : time.format(ADMIN_DATE_TIME_FORMATTER);
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

  private BigDecimal calculateActualRefundAmount(BigDecimal orderAmount, BigDecimal cashbackDeduction) {
    BigDecimal total = orderAmount == null ? BigDecimal.ZERO : orderAmount;
    BigDecimal deduction = cashbackDeduction == null ? BigDecimal.ZERO : cashbackDeduction;
    BigDecimal actual = total.subtract(deduction);
    if (actual.compareTo(BigDecimal.ZERO) < 0) {
      actual = BigDecimal.ZERO;
    }
    return actual.setScale(2, RoundingMode.HALF_UP);
  }

  private String appendCashbackDeductionReason(String reason, BigDecimal cashbackDeduction, BigDecimal refundAmount) {
    if (cashbackDeduction == null || cashbackDeduction.compareTo(BigDecimal.ZERO) <= 0) {
      return reason;
    }
    return trimReason(reason + " | cashback deducted "
        + cashbackDeduction.setScale(2, RoundingMode.HALF_UP)
        + ", actual refund "
        + refundAmount.setScale(2, RoundingMode.HALF_UP));
  }

  private String mapRefundStatus(Status status) {
    if (Status.SUCCESS.equals(status)) {
      return "SUCCESS";
    }
    if (Status.CLOSED.equals(status) || Status.ABNORMAL.equals(status)) {
      return "FAILED";
    }
    return "PROCESSING";
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

  private AdminUserVO toAdminUserVO(User user) {
    AdminUserVO vo = new AdminUserVO();
    vo.setId(user.getId());
    vo.setPhone(user.getPhone());
    vo.setNickname(user.getNickname());
    vo.setInviteCode(user.getInviteCode());
    vo.setInviterId(user.getInviterId());
    vo.setStatus(user.getStatus());
    vo.setWechatWebOpenid(resolveWechatWebOpenid(user.getId()));
    vo.setWechatMiniappOpenid(resolveWechatMiniappOpenid(user.getId()));
    vo.setCreatedAt(user.getCreatedAt());
    return vo;
  }

  private String resolveWechatWebOpenid(Long userId) {
    if (userId == null) {
      return "";
    }
    UserWechatAuth auth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_WEB);
    if (auth == null || auth.getOpenid() == null || auth.getOpenid().isBlank()) {
      return "";
    }
    return auth.getOpenid().trim();
  }

  private String resolveWechatMiniappOpenid(Long userId) {
    if (userId == null) {
      return "";
    }
    User user = userMapper.findById(userId);
    if (user != null && user.getMiniappOpenid() != null && !user.getMiniappOpenid().isBlank()) {
      return user.getMiniappOpenid().trim();
    }
    UserWechatAuth auth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_MINIAPP);
    if (auth == null || auth.getOpenid() == null || auth.getOpenid().isBlank()) {
      return "";
    }
    return auth.getOpenid().trim();
  }

  private boolean isWechatPaidOrder(Order order) {
    return order != null
        && "WECHAT".equals(order.getPayType())
        && ((order.getTransactionId() != null && !order.getTransactionId().isBlank())
        || (order.getOrderNo() != null && !order.getOrderNo().isBlank()));
  }

  private String buildShippingItemDesc(Order order) {
    String productName = order == null || order.getProductName() == null || order.getProductName().isBlank()
        ? "商品"
        : order.getProductName().trim();
    int quantity = order == null || order.getQuantity() == null || order.getQuantity() < 1 ? 1 : order.getQuantity();
    return productName + " * " + quantity;
  }

  private String normalizeRequiredText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(message);
    }
    return value.trim();
  }
}
