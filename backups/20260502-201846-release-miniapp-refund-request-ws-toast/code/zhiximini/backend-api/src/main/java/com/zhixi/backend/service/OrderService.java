package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.CreateOrderRequest;
import com.zhixi.backend.mapper.CashbackDebtMapper;
import com.zhixi.backend.mapper.InviteProductRelationMapper;
import com.zhixi.backend.mapper.OrderMapper;
import com.zhixi.backend.mapper.UserWechatAuthMapper;
import com.zhixi.backend.model.CashbackDebt;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.InviteProductRelation;
import com.zhixi.backend.model.Order;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.UserWechatAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);
  private static final String WECHAT_SOURCE_MINIAPP = "MINIAPP";
  private static final long REFUND_CANCEL_WINDOW_DAYS = 7L;
  private static final String REFUND_CANCEL_REASON = "Refund within 7 days cancels cashback";
  private static final BigDecimal REMOTE_SHIPPING_FEE = BigDecimal.valueOf(20);
  private static final Set<String> REMOTE_REGIONS = new HashSet<>(Arrays.asList(
      "香港",
      "香港特别行政区",
      "澳门",
      "澳门特别行政区",
      "台湾",
      "台湾省",
      "青海",
      "青海省",
      "西藏",
      "西藏自治区",
      "新疆",
      "新疆维吾尔自治区",
      "内蒙古",
      "内蒙古自治区"
  ));

  private final OrderMapper orderMapper;
  private final UserService userService;
  private final ProductService productService;
  private final InviteProductRelationMapper inviteProductRelationMapper;
  private final CashbackService cashbackService;
  private final UserWechatAuthMapper userWechatAuthMapper;
  private final CashbackDebtMapper cashbackDebtMapper;
  private final TransactionTemplate paymentTransactionTemplate;
  private final WithdrawalWebSocketService withdrawalWebSocketService;

  public OrderService(
      OrderMapper orderMapper,
      UserService userService,
      ProductService productService,
      InviteProductRelationMapper inviteProductRelationMapper,
      CashbackService cashbackService,
      UserWechatAuthMapper userWechatAuthMapper,
      CashbackDebtMapper cashbackDebtMapper,
      PlatformTransactionManager transactionManager,
      WithdrawalWebSocketService withdrawalWebSocketService
  ) {
    this.orderMapper = orderMapper;
    this.userService = userService;
    this.productService = productService;
    this.inviteProductRelationMapper = inviteProductRelationMapper;
    this.cashbackService = cashbackService;
    this.userWechatAuthMapper = userWechatAuthMapper;
    this.cashbackDebtMapper = cashbackDebtMapper;
    this.paymentTransactionTemplate = new TransactionTemplate(transactionManager);
    this.paymentTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.withdrawalWebSocketService = withdrawalWebSocketService;
  }

  @Transactional
  public Order createOrder(Long userId, CreateOrderRequest request) {
    User currentUser = userService.getUser(userId);
    userService.ensureEnabled(currentUser);
    if (request.getInviterId() != null) {
      userService.bindInviterByIdIfNeeded(userId, request.getInviterId());
    }
    Product product = productService.getById(request.getProductId());

    BigDecimal productAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
    String province = normalizeText(request.getProvince());
    BigDecimal shippingFee = isRemoteRegion(province, request.getAddress()) ? REMOTE_SHIPPING_FEE : BigDecimal.ZERO;

    Order order = new Order();
    order.setUserId(userId);
    order.setProductId(request.getProductId());
    order.setQuantity(request.getQuantity());
    order.setRecipientName(request.getRecipientName());
    order.setRecipientPhone(request.getRecipientPhone());
    order.setProvince(province);
    order.setCity(normalizeText(request.getCity()));
    order.setDistrict(normalizeText(request.getDistrict()));
    order.setAddress(request.getAddress());
    order.setStatus("PENDING");
    order.setProductAmount(productAmount);
    order.setShippingFee(shippingFee);
    order.setCashbackBaseAmount(productAmount);
    order.setTotalAmount(productAmount.add(shippingFee));
    orderMapper.insert(order);
    return orderMapper.findById(order.getId());
  }

  public Order markPaid(Long orderId) {
    return markPaid(orderId, LocalDateTime.now(), "MOCK", "MOCK_" + System.currentTimeMillis());
  }

  public Order markPaid(Long orderId, LocalDateTime paidAt, String paymentMethod, String transactionId) {
    return markPaid(orderId, paidAt, paymentMethod, transactionId, null);
  }

  public Order markPaid(Long orderId, LocalDateTime paidAt, String paymentMethod, String transactionId, String payerOpenid) {
    MarkPaidResult result = paymentTransactionTemplate.execute(status -> {
      Order current = getOrder(orderId);
      if (isAlreadyPaid(current) && transactionId != null && transactionId.equals(current.getTransactionId())) {
        return new MarkPaidResult(current, false);
      }
      if (!"PENDING".equals(current.getStatus())) {
        throw new BusinessException("Only pending orders can be paid");
      }

      int updated = orderMapper.markPaid(orderId, paidAt, paymentMethod, transactionId);
      if (updated <= 0) {
        throw new BusinessException("Failed to update order payment status");
      }

      return new MarkPaidResult(orderMapper.findById(orderId), true);
    });
    if (result == null) {
      throw new BusinessException("Failed to update order payment status");
    }

    Order order = result.getOrder();
    recordPaymentOpenid(order.getUserId(), payerOpenid);

    if (result.isNewlyPaid()) {
      settlePaidOrder(order, paidAt, transactionId);
    }

    return order;
  }

  private void settlePaidOrder(Order order, LocalDateTime paidAt, String transactionId) {
    try {
      paymentTransactionTemplate.executeWithoutResult(status -> applyPostPaymentSettlement(order, paidAt));
    } catch (Exception ex) {
      log.error(
          "Post-payment settlement failed; payment status remains paid. orderId={}, orderNo={}, transactionId={}",
          order.getId(),
          order.getOrderNo(),
          transactionId,
          ex
      );
    }
  }

  private void applyPostPaymentSettlement(Order order, LocalDateTime paidAt) {
    productService.incrementSalesCount(order.getProductId(), order.getQuantity());

    Product product = productService.getAnyById(order.getProductId());
    User payingUser = userService.getUser(order.getUserId());
    int paidBefore = orderMapper.countPaidBeforeOrderByProduct(
        order.getUserId(),
        order.getProductId(),
        order.getId(),
        payingUser.getCashbackResetAt()
    );
    int paidSeq = paidBefore + 1;
    cashbackService.grantPersonalCashback(
        order.getUserId(),
        order.getId(),
        order.getProductId(),
        product,
        paidSeq,
        cashbackBaseAmountOf(order)
    );

    if (payingUser.getInviterId() == null) {
      return;
    }

    inviteProductRelationMapper.insertIgnore(payingUser.getInviterId(), order.getUserId(), order.getProductId());
    int firstPaidMarked = inviteProductRelationMapper.markFirstPaid(order.getUserId(), order.getProductId(), paidAt);
    if (firstPaidMarked > 0) {
      int firstPaidCount = inviteProductRelationMapper.countInviteeFirstPaid(payingUser.getInviterId(), order.getProductId());
      cashbackService.grantInviteBatchCashback(
          payingUser.getInviterId(),
          order.getProductId(),
          product,
          firstPaidCount,
          cashbackBaseAmountOf(order),
          order.getId()
      );
    }
  }

  private static final class MarkPaidResult {
    private final Order order;
    private final boolean newlyPaid;

    private MarkPaidResult(Order order, boolean newlyPaid) {
      this.order = order;
      this.newlyPaid = newlyPaid;
    }

    private Order getOrder() {
      return order;
    }

    private boolean isNewlyPaid() {
      return newlyPaid;
    }
  }

  @Transactional
  public void recordPaymentOpenid(Long userId, String payerOpenid) {
    if (payerOpenid == null || payerOpenid.isBlank()) {
      return;
    }

    try {
      User user = userService.getUser(userId);
      String openid = payerOpenid.trim();
      User owner = userService.findByMiniappOpenid(openid);
      if (owner != null && !owner.getId().equals(userId)) {
        log.warn("Skip payment openid sync because openid belongs to another user, userId={}, ownerUserId={}", userId, owner.getId());
        return;
      }

      if (user.getMiniappOpenid() != null && !user.getMiniappOpenid().isBlank()) {
        if (!openid.equals(user.getMiniappOpenid())) {
          log.warn("Replace user miniapp openid with paid payer openid, userId={}", userId);
          userService.updateMiniappOpenid(userId, openid);
        }
        ensureWechatAuth(userId, openid);
        return;
      }

      userService.updateMiniappOpenid(userId, openid);
      ensureWechatAuth(userId, openid);
    } catch (Exception ex) {
      log.warn("Payment openid sync failed, userId={}", userId, ex);
    }
  }

  private void ensureWechatAuth(Long userId, String openid) {
    UserWechatAuth existing = userWechatAuthMapper.findByOpenid(openid);
    if (existing != null) {
      if (!userId.equals(existing.getUserId())) {
        log.warn("Skip user_wechat_auth insert because openid is bound to another user, userId={}, ownerUserId={}", userId, existing.getUserId());
      }
      return;
    }

    UserWechatAuth auth = new UserWechatAuth();
    auth.setUserId(userId);
    auth.setSourceType(WECHAT_SOURCE_MINIAPP);
    auth.setOpenid(openid);
    userWechatAuthMapper.insert(auth);
  }

  public List<Order> listByUser(Long userId) {
    userService.getUser(userId);
    return orderMapper.findByUserId(userId);
  }

  public List<Order> listAll() {
    return orderMapper.findAll();
  }

  @Transactional
  public void createRefundRequest(Long orderId, Long userId, String reason) {
    Order order = getOrder(orderId);
    if (!order.getUserId().equals(userId)) {
      throw new BusinessException("订单不存在");
    }
    if (!"PAID".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
      throw new BusinessException("当前订单状态不可申请退款");
    }
    if ("PROCESSING".equals(order.getRefundStatus()) || "SUCCESS".equals(order.getRefundStatus())) {
      throw new BusinessException("当前订单已有退款在处理中或已退款，请勿重复申请");
    }
    if ("PENDING".equals(order.getRefundRequestStatus())) {
      throw new BusinessException("您已提交退款申请，请等待审批");
    }
    String normalizedReason = reason == null || reason.isBlank() ? "" : reason.trim();
    if (normalizedReason.isEmpty()) {
      throw new BusinessException("退款理由不能为空");
    }
    if (normalizedReason.length() > 200) {
      throw new BusinessException("退款理由最长200字");
    }

    int updated = orderMapper.insertRefundRequest(orderId, normalizedReason);
    if (updated <= 0) {
      throw new BusinessException("提交退款申请失败，请稍后重试");
    }

    Order updatedOrder = orderMapper.findById(orderId);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("orderId", updatedOrder.getId());
    payload.put("orderNo", updatedOrder.getOrderNo());
    payload.put("userId", updatedOrder.getUserId());
    payload.put("refundRequestReason", updatedOrder.getRefundRequestReason());
    payload.put("refundRequestAt", updatedOrder.getRefundRequestAt() != null ? updatedOrder.getRefundRequestAt().toString() : null);
    payload.put("refundRequestStatus", updatedOrder.getRefundRequestStatus());
    withdrawalWebSocketService.broadcast("refund-request-created", payload);
  }

  public double totalPaidAmount() {
    Double value = orderMapper.totalPaidAmount();
    return value == null ? 0D : value;
  }

  public Order getOrder(Long orderId) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BusinessException("Order not found");
    }
    return order;
  }

  public Order getOrderByOrderNo(String orderNo) {
    Order order = orderMapper.findByOrderNo(orderNo);
    if (order == null) {
      throw new BusinessException("Order not found");
    }
    return order;
  }

  public Order getOrderForUser(Long orderId, Long userId) {
    Order order = getOrder(orderId);
    if (!order.getUserId().equals(userId)) {
      throw new BusinessException("Order not found");
    }
    return order;
  }

  public void validateRefundCashbackReversible(Order order, LocalDateTime refundApplyAt) {
    if (!shouldCancelCashback(order, refundApplyAt)) {
      return;
    }

    for (CashbackRecord record : collectCashbacksToCancel(order)) {
      if (cashbackService.isTransferLocked(record)) {
        throw new BusinessException("Cashback is already locked for transfer; handle cashback before refund");
      }
    }
  }

  public BigDecimal calculateRefundCashbackDeduction(Order order) {
    BigDecimal amount = BigDecimal.ZERO;
    for (CashbackRecord record : collectRefundAffectedCashbacks(order)) {
      if (cashbackService.isTransferLocked(record) && record.getAmount() != null) {
        amount = amount.add(record.getAmount());
      }
    }
    BigDecimal orderAmount = order == null || order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    return amount.min(orderAmount);
  }

  @Transactional
  public void handleRefundSuccessByOrderId(Long orderId) {
    Order order = getOrder(orderId);
    cancelCashbackForRefundIfNeeded(order);
  }

  @Transactional
  public void markRefundSuccessByRefundNo(String refundNo, String refundId) {
    if (refundNo == null || refundNo.isBlank()) {
      return;
    }
    int updated = orderMapper.markRefundSuccessByRefundNo(refundNo, refundId);
    if (updated <= 0) {
      log.warn("Skip refund success callback because refundNo was not matched, refundNo={}", refundNo);
      return;
    }

    Order order = orderMapper.findByRefundNo(refundNo);
    if (order != null) {
      cancelCashbackForRefundIfNeeded(order);
    }
  }

  @Transactional
  public void markRefundFailedByRefundNo(String refundNo, String refundId) {
    if (refundNo == null || refundNo.isBlank()) {
      return;
    }
    int updated = orderMapper.markRefundFailedByRefundNo(refundNo, refundId);
    if (updated <= 0) {
      log.warn("Skip refund failure callback because refundNo was not matched, refundNo={}", refundNo);
    }
  }

  private boolean isAlreadyPaid(Order order) {
    String status = order.getStatus();
    return "PAID".equals(status) || "SHIPPED".equals(status) || "COMPLETED".equals(status);
  }

  private void cancelCashbackForRefundIfNeeded(Order order) {
    if (!shouldCancelCashback(order, order.getRefundApplyAt())) {
      return;
    }

    for (CashbackRecord record : cashbackService.listByOrder(order.getId())) {
      if (!cashbackService.isTransferLocked(record)) {
        cashbackService.cancelForRefund(record, REFUND_CANCEL_REASON);
      } else if (!"CANCELLED".equals(record.getStatus())) {
        createDebtForLockedCashback(record, order.getId(), REFUND_CANCEL_REASON);
      }
    }

    Product product = productService.getAnyById(order.getProductId());
    recalculatePersonalCashbackAfterRefund(order.getUserId(), order.getProductId(), product);

    InviteProductRelation relation = inviteProductRelationMapper.findByInviteeIdAndProductId(order.getUserId(), order.getProductId());
    if (!isTrackedFirstPaidOrder(relation, order)) {
      return;
    }

    LocalDateTime nextFirstPaidAt =
        findReplacementFirstPaidTimeExcludingOrder(order.getUserId(), order.getProductId(), order.getId());
    if (nextFirstPaidAt == null) {
      int cleared = inviteProductRelationMapper.clearFirstPaid(order.getUserId(), order.getProductId(), relation.getFirstPaidAt());
      if (cleared <= 0) {
        return;
      }
    } else {
      inviteProductRelationMapper.updateFirstPaid(order.getUserId(), order.getProductId(), nextFirstPaidAt);
    }

    reconcileInviteBatchCashbackAfterRefund(relation.getInviterId(), order.getProductId(), order.getId());
  }

  private void createDebtForLockedCashback(CashbackRecord record, Long orderId, String reason) {
    CashbackDebt debt = new CashbackDebt();
    debt.setUserId(record.getUserId());
    debt.setOrderId(orderId);
    debt.setCashbackId(record.getId());
    debt.setAmount(record.getAmount());
    debt.setReason(reason);
    debt.setStatus("PENDING");
    cashbackDebtMapper.insert(debt);
  }

  private void recalculatePersonalCashbackAfterRefund(Long userId, Long productId, Product product) {
    List<Order> validOrders = orderMapper.findValidPaidByUserAndProductOrderByIdAsc(userId, productId);
    if (validOrders.isEmpty()) {
      return;
    }

    for (int i = 0; i < validOrders.size(); i++) {
      Order order = validOrders.get(i);
      int newSeq = i + 1;
      BigDecimal expectedAmount =
          cashbackService.calculatePersonalCashbackAmount(newSeq, cashbackBaseAmountOf(order), product);

      List<CashbackRecord> existingRecords = cashbackService.listByOrder(order.getId());
      CashbackRecord personalRecord = existingRecords.stream()
          .filter(r -> "PERSONAL_ORDER".equals(r.getType()))
          .findFirst().orElse(null);

      if (expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
        if (personalRecord != null && !cashbackService.isTransferLocked(personalRecord)) {
          cashbackService.cancelForRefund(personalRecord, "Refund recalculation: seq " + newSeq + " no longer earns cashback");
        }
      } else {
        if (personalRecord == null) {
          cashbackService.grantPersonalCashback(
              userId,
              order.getId(),
              order.getProductId(),
              product,
              newSeq,
              cashbackBaseAmountOf(order)
          );
        } else if (!cashbackService.isTransferLocked(personalRecord) && personalRecord.getAmount().compareTo(expectedAmount) != 0) {
          cashbackService.cancelForRefund(
              personalRecord,
              "Refund recalculation: amount adjusted from " + personalRecord.getAmount() + " to " + expectedAmount
          );
          cashbackService.grantPersonalCashback(
              userId,
              order.getId(),
              order.getProductId(),
              product,
              newSeq,
              cashbackBaseAmountOf(order)
          );
        }
      }
    }
  }

  private void reconcileInviteBatchCashbackAfterRefund(Long inviterId, Long productId, Long orderId) {
    if (inviterId == null || productId == null) {
      return;
    }

    Product product = productService.getAnyById(productId);
    int batchSize = cashbackService.resolveInviteBatchSize(product);
    List<InviteProductRelation> firstPaidRelations =
        inviteProductRelationMapper.findFirstPaidByInviterAndProduct(inviterId, productId);
    int expectedBatchCount = firstPaidRelations.size() / batchSize;
    List<CashbackRecord> batchRecords = cashbackService.listInviteBatchByUserAndProduct(inviterId, productId);
    Map<Integer, CashbackRecord> activeBatchRecords = new LinkedHashMap<>();

    for (CashbackRecord record : batchRecords) {
      Integer batchNo = record.getBatchNo();
      if (batchNo == null) {
        continue;
      }
      if (!"CANCELLED".equalsIgnoreCase(record.getStatus()) && !activeBatchRecords.containsKey(batchNo)) {
        activeBatchRecords.put(batchNo, record);
      }
      if (batchNo <= expectedBatchCount || "CANCELLED".equalsIgnoreCase(record.getStatus())) {
        continue;
      }
      if (cashbackService.isTransferLocked(record)) {
        createDebtForLockedCashback(record, orderId, REFUND_CANCEL_REASON);
      } else {
        cashbackService.cancelForRefund(record, REFUND_CANCEL_REASON);
      }
    }

    for (int batchNo = 1; batchNo <= expectedBatchCount; batchNo++) {
      if (activeBatchRecords.containsKey(batchNo)) {
        continue;
      }
      int triggerIndex = batchNo * batchSize - 1;
      if (triggerIndex < 0 || triggerIndex >= firstPaidRelations.size()) {
        continue;
      }
      InviteProductRelation triggerRelation = firstPaidRelations.get(triggerIndex);
      Order triggerOrder = orderMapper.findFirstValidPaidByUserAndProduct(triggerRelation.getInviteeId(), productId);
      if (triggerOrder == null) {
        continue;
      }
      cashbackService.grantInviteBatchCashback(
          inviterId,
          productId,
          product,
          batchNo * batchSize,
          cashbackBaseAmountOf(triggerOrder),
          triggerOrder.getId()
      );
    }
  }

  public List<CashbackRecord> collectRefundAffectedCashbacks(Order order) {
    return collectCashbacksForRefundProcessing(order);
  }

  private List<CashbackRecord> collectCashbacksToCancel(Order order) {
    return collectCashbacksForRefundProcessing(order);
  }

  private List<CashbackRecord> collectCashbacksForRefundProcessing(Order order) {
    Map<Long, CashbackRecord> records = new LinkedHashMap<>();
    for (CashbackRecord record : cashbackService.listByOrder(order.getId())) {
      if (record.getId() != null) {
        records.put(record.getId(), record);
      }
    }
    for (CashbackRecord record : collectInviteBatchCashbacksAffectedByRefund(order)) {
      if (record.getId() != null) {
        records.put(record.getId(), record);
      }
    }
    return new ArrayList<>(records.values());
  }

  private List<CashbackRecord> collectInviteBatchCashbacksAffectedByRefund(Order order) {
    List<CashbackRecord> records = new ArrayList<>();
    if (order == null || order.getProductId() == null) {
      return records;
    }

    InviteProductRelation relation =
        inviteProductRelationMapper.findByInviteeIdAndProductId(order.getUserId(), order.getProductId());
    if (!isTrackedFirstPaidOrder(relation, order)) {
      return records;
    }

    Product product = productService.getAnyById(order.getProductId());
    int batchSize = cashbackService.resolveInviteBatchSize(product);
    int currentFirstPaidCount =
        inviteProductRelationMapper.countInviteeFirstPaid(relation.getInviterId(), order.getProductId());
    LocalDateTime replacementFirstPaidAt =
        findReplacementFirstPaidTimeExcludingOrder(order.getUserId(), order.getProductId(), order.getId());
    int adjustedFirstPaidCount = replacementFirstPaidAt == null
        ? Math.max(currentFirstPaidCount - 1, 0)
        : currentFirstPaidCount;
    int expectedBatchCount = adjustedFirstPaidCount / batchSize;

    for (CashbackRecord record : cashbackService.listInviteBatchByUserAndProduct(relation.getInviterId(), order.getProductId())) {
      Integer batchNo = record.getBatchNo();
      if (batchNo != null && batchNo > expectedBatchCount && !"CANCELLED".equalsIgnoreCase(record.getStatus())) {
        records.add(record);
      }
    }
    return records;
  }

  private boolean shouldCancelCashback(Order order, LocalDateTime refundApplyAt) {
    if (order == null || order.getPaidAt() == null || refundApplyAt == null) {
      return false;
    }
    return !refundApplyAt.isAfter(order.getPaidAt().plusDays(REFUND_CANCEL_WINDOW_DAYS));
  }

  private boolean isTrackedFirstPaidOrder(InviteProductRelation relation, Order order) {
    return relation != null
        && relation.getInviterId() != null
        && relation.getFirstPaidAt() != null
        && order.getPaidAt() != null
        && relation.getFirstPaidAt().equals(order.getPaidAt());
  }

  private LocalDateTime findReplacementFirstPaidTimeExcludingOrder(Long userId, Long productId, Long excludedOrderId) {
    List<Order> validOrders = orderMapper.findValidPaidByUserAndProductOrderByIdAsc(userId, productId);
    for (Order candidate : validOrders) {
      if (candidate.getId() != null && candidate.getId().equals(excludedOrderId)) {
        continue;
      }
      if (candidate.getPaidAt() != null) {
        return candidate.getPaidAt();
      }
    }
    return null;
  }

  private BigDecimal cashbackBaseAmountOf(Order order) {
    if (order == null) {
      return BigDecimal.ZERO;
    }
    if (order.getCashbackBaseAmount() != null) {
      return order.getCashbackBaseAmount();
    }
    if (order.getProductAmount() != null) {
      return order.getProductAmount();
    }
    return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
  }

  private boolean isRemoteRegion(String province, String address) {
    if (!normalizeText(province).isEmpty() && REMOTE_REGIONS.contains(normalizeText(province))) {
      return true;
    }
    String fullAddress = normalizeText(address);
    if (fullAddress.isEmpty()) {
      return false;
    }
    for (String region : REMOTE_REGIONS) {
      if (fullAddress.startsWith(region)) {
        return true;
      }
    }
    return false;
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }
}
