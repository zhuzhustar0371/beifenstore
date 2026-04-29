package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.CreateOrderRequest;
import com.zhixi.backend.mapper.InviteRelationMapper;
import com.zhixi.backend.mapper.OrderMapper;
import com.zhixi.backend.mapper.UserWechatAuthMapper;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.Order;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.UserWechatAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);
  private static final String WECHAT_SOURCE_MINIAPP = "MINIAPP";
  private static final long REFUND_CANCEL_WINDOW_DAYS = 7L;
  private static final String REFUND_CANCEL_REASON = "7天内退款取消返现";

  private final OrderMapper orderMapper;
  private final UserService userService;
  private final ProductService productService;
  private final InviteRelationMapper inviteRelationMapper;
  private final CashbackService cashbackService;
  private final UserWechatAuthMapper userWechatAuthMapper;

  public OrderService(
      OrderMapper orderMapper,
      UserService userService,
      ProductService productService,
      InviteRelationMapper inviteRelationMapper,
      CashbackService cashbackService,
      UserWechatAuthMapper userWechatAuthMapper
  ) {
    this.orderMapper = orderMapper;
    this.userService = userService;
    this.productService = productService;
    this.inviteRelationMapper = inviteRelationMapper;
    this.cashbackService = cashbackService;
    this.userWechatAuthMapper = userWechatAuthMapper;
  }

  @Transactional
  public Order createOrder(Long userId, CreateOrderRequest request) {
    userService.getUser(userId);
    if (request.getInviterId() != null) {
      userService.bindInviterByIdIfNeeded(userId, request.getInviterId());
    }
    Product product = productService.getById(request.getProductId());

    Order order = new Order();
    order.setUserId(userId);
    order.setProductId(request.getProductId());
    order.setQuantity(request.getQuantity());
    order.setRecipientName(request.getRecipientName());
    order.setRecipientPhone(request.getRecipientPhone());
    order.setAddress(request.getAddress());
    order.setStatus("PENDING");
    order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
    orderMapper.insert(order);
    return orderMapper.findById(order.getId());
  }

  @Transactional
  public Order markPaid(Long orderId) {
    return markPaid(orderId, LocalDateTime.now(), "MOCK", "MOCK_" + System.currentTimeMillis());
  }

  @Transactional
  public Order markPaid(Long orderId, LocalDateTime paidAt, String paymentMethod, String transactionId) {
    return markPaid(orderId, paidAt, paymentMethod, transactionId, null);
  }

  @Transactional
  public Order markPaid(Long orderId, LocalDateTime paidAt, String paymentMethod, String transactionId, String payerOpenid) {
    Order current = getOrder(orderId);
    if (isAlreadyPaid(current) && transactionId != null && transactionId.equals(current.getTransactionId())) {
      recordPaymentOpenid(current.getUserId(), payerOpenid);
      return current;
    }
    if (!"PENDING".equals(current.getStatus())) {
      throw new BusinessException("仅待支付订单可支付");
    }

    int updated = orderMapper.markPaid(orderId, paidAt, paymentMethod, transactionId);
    if (updated <= 0) {
      throw new BusinessException("订单支付状态更新失败");
    }

    Order order = orderMapper.findById(orderId);
    recordPaymentOpenid(order.getUserId(), payerOpenid);
    productService.incrementSalesCount(order.getProductId(), order.getQuantity());

    User payingUser = userService.getUser(order.getUserId());
    int paidBefore = orderMapper.countPaidBeforeOrder(order.getUserId(), order.getId(), payingUser.getCashbackResetAt());
    int paidSeq = paidBefore + 1;
    cashbackService.grantPersonalCashback(order.getUserId(), order.getId(), paidSeq, order.getTotalAmount());

    int firstPaidMarked = inviteRelationMapper.markFirstPaid(order.getUserId(), paidAt);
    if (firstPaidMarked > 0) {
      if (payingUser.getInviterId() != null) {
        int firstPaidCount = inviteRelationMapper.countInviteeFirstPaid(payingUser.getInviterId());
        cashbackService.grantInviteBatchCashback(payingUser.getInviterId(), firstPaidCount, order.getTotalAmount());
      }
    }

    return order;
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

  public double totalPaidAmount() {
    Double value = orderMapper.totalPaidAmount();
    return value == null ? 0D : value;
  }

  public Order getOrder(Long orderId) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BusinessException("订单不存在");
    }
    return order;
  }

  public Order getOrderByOrderNo(String orderNo) {
    Order order = orderMapper.findByOrderNo(orderNo);
    if (order == null) {
      throw new BusinessException("订单不存在");
    }
    return order;
  }

  public Order getOrderForUser(Long orderId, Long userId) {
    Order order = getOrder(orderId);
    if (!order.getUserId().equals(userId)) {
      throw new BusinessException("订单不存在");
    }
    return order;
  }

  public void validateRefundCashbackReversible(Order order, LocalDateTime refundApplyAt) {
    if (!shouldCancelCashback(order, refundApplyAt)) {
      return;
    }

    for (CashbackRecord record : collectCashbacksToCancel(order)) {
      if (cashbackService.isTransferLocked(record)) {
        throw new BusinessException("7天内退款需取消返现，但关联返现已发起打款或已到账，请先人工处理返现后再退款");
      }
    }
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
      cashbackService.cancelForRefund(record, REFUND_CANCEL_REASON);
    }

    recalculatePersonalCashbackAfterRefund(order.getUserId());

    InviteRelation relation = inviteRelationMapper.findByInviteeId(order.getUserId());
    if (!isTrackedFirstPaidOrder(relation, order)) {
      return;
    }

    int currentFirstPaidCount = inviteRelationMapper.countInviteeFirstPaid(relation.getInviterId());
    int validFirstPaidCount = Math.max(currentFirstPaidCount - 1, 0);
    int cleared = inviteRelationMapper.clearFirstPaid(order.getUserId(), relation.getFirstPaidAt());
    if (cleared <= 0) {
      return;
    }

    int validBatchCount = validFirstPaidCount / 3;
    for (CashbackRecord record : cashbackService.listInviteBatchByUser(relation.getInviterId())) {
      Integer batchNo = record.getBatchNo();
      if (batchNo != null && batchNo > validBatchCount) {
        cashbackService.cancelForRefund(record, REFUND_CANCEL_REASON);
      }
    }
  }

  private void recalculatePersonalCashbackAfterRefund(Long userId) {
    List<Order> validOrders = orderMapper.findValidPaidByUserOrderByIdAsc(userId);
    if (validOrders.isEmpty()) {
      return;
    }

    for (int i = 0; i < validOrders.size(); i++) {
      Order order = validOrders.get(i);
      int newSeq = i + 1;
      BigDecimal expectedAmount = cashbackService.calculatePersonalCashbackAmount(newSeq, order.getTotalAmount());

      List<CashbackRecord> existingRecords = cashbackService.listByOrder(order.getId());
      CashbackRecord personalRecord = existingRecords.stream()
          .filter(r -> "PERSONAL_ORDER".equals(r.getType()))
          .findFirst().orElse(null);

      if (expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
        if (personalRecord != null) {
          cashbackService.cancelForRefund(personalRecord, "退款重新计算：第" + newSeq + "单不返现");
        }
      } else {
        if (personalRecord == null) {
          cashbackService.grantPersonalCashback(userId, order.getId(), newSeq, order.getTotalAmount());
        } else if (personalRecord.getAmount().compareTo(expectedAmount) != 0) {
          cashbackService.cancelForRefund(personalRecord,
              "退款重新计算：金额由" + personalRecord.getAmount() + "调整为" + expectedAmount);
          cashbackService.grantPersonalCashback(userId, order.getId(), newSeq, order.getTotalAmount());
        }
      }
    }
  }

  private List<CashbackRecord> collectCashbacksToCancel(Order order) {
    List<CashbackRecord> records = new ArrayList<>(cashbackService.listByOrder(order.getId()));
    InviteRelation relation = inviteRelationMapper.findByInviteeId(order.getUserId());
    if (!isTrackedFirstPaidOrder(relation, order)) {
      return records;
    }

    int currentFirstPaidCount = inviteRelationMapper.countInviteeFirstPaid(relation.getInviterId());
    int validBatchCount = Math.max(currentFirstPaidCount - 1, 0) / 3;
    for (CashbackRecord record : cashbackService.listInviteBatchByUser(relation.getInviterId())) {
      Integer batchNo = record.getBatchNo();
      if (batchNo != null && batchNo > validBatchCount) {
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

  private boolean isTrackedFirstPaidOrder(InviteRelation relation, Order order) {
    return relation != null
        && relation.getInviterId() != null
        && relation.getFirstPaidAt() != null
        && order.getPaidAt() != null
        && relation.getFirstPaidAt().equals(order.getPaidAt());
  }
}
