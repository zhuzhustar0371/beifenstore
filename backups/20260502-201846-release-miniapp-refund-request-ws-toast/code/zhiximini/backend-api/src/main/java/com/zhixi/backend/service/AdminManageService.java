package com.zhixi.backend.service;

import com.wechat.pay.java.service.refund.model.Status;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.AdminCashbackVO;
import com.zhixi.backend.dto.AdminInviteVO;
import com.zhixi.backend.dto.AdminOrderVO;
import com.zhixi.backend.dto.AdminPageResult;
import com.zhixi.backend.dto.AdminProductUpsertRequest;
import com.zhixi.backend.dto.AdminUserVO;
import com.zhixi.backend.dto.AdminWithdrawalRequestVO;
import com.zhixi.backend.mapper.CashbackRecordMapper;
import com.zhixi.backend.mapper.InviteProductRelationMapper;
import com.zhixi.backend.mapper.InviteRelationMapper;
import com.zhixi.backend.mapper.OrderMapper;
import com.zhixi.backend.mapper.ProductMapper;
import com.zhixi.backend.mapper.ShippingRecordMapper;
import com.zhixi.backend.mapper.UserMapper;
import com.zhixi.backend.mapper.UserSessionMapper;
import com.zhixi.backend.mapper.UserWechatAuthMapper;
import com.zhixi.backend.mapper.WithdrawalRequestMapper;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.Order;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.UserWechatAuth;
import com.zhixi.backend.model.WithdrawalRequest;
import com.zhixi.backend.model.WithdrawalRequestItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminManageService {
  private static final String WECHAT_SOURCE_MINIAPP = "MINIAPP";
  private static final String WECHAT_SOURCE_WEB = "WEB";
  private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final UserMapper userMapper;
  private final ProductMapper productMapper;
  private final InviteRelationMapper inviteRelationMapper;
  private final InviteProductRelationMapper inviteProductRelationMapper;
  private final OrderMapper orderMapper;
  private final CashbackRecordMapper cashbackRecordMapper;
  private final ShippingRecordMapper shippingRecordMapper;
  private final UserSessionMapper userSessionMapper;
  private final UserWechatAuthMapper userWechatAuthMapper;
  private final WithdrawalRequestMapper withdrawalRequestMapper;
  private final WechatPayService wechatPayService;
  private final WechatTradeManagementService wechatTradeManagementService;
  private final OrderService orderService;
  private final CashbackService cashbackService;
  private final WithdrawalEventService withdrawalEventService;
  private final JdbcTemplate jdbcTemplate;

  public AdminManageService(
      UserMapper userMapper,
      ProductMapper productMapper,
      InviteRelationMapper inviteRelationMapper,
      InviteProductRelationMapper inviteProductRelationMapper,
      OrderMapper orderMapper,
      CashbackRecordMapper cashbackRecordMapper,
      ShippingRecordMapper shippingRecordMapper,
      UserSessionMapper userSessionMapper,
      UserWechatAuthMapper userWechatAuthMapper,
      WithdrawalRequestMapper withdrawalRequestMapper,
      WechatPayService wechatPayService,
      WechatTradeManagementService wechatTradeManagementService,
      OrderService orderService,
      CashbackService cashbackService,
      WithdrawalEventService withdrawalEventService,
      JdbcTemplate jdbcTemplate
  ) {
    this.userMapper = userMapper;
    this.productMapper = productMapper;
    this.inviteRelationMapper = inviteRelationMapper;
    this.inviteProductRelationMapper = inviteProductRelationMapper;
    this.orderMapper = orderMapper;
    this.cashbackRecordMapper = cashbackRecordMapper;
    this.shippingRecordMapper = shippingRecordMapper;
    this.userSessionMapper = userSessionMapper;
    this.userWechatAuthMapper = userWechatAuthMapper;
    this.withdrawalRequestMapper = withdrawalRequestMapper;
    this.wechatPayService = wechatPayService;
    this.wechatTradeManagementService = wechatTradeManagementService;
    this.orderService = orderService;
    this.cashbackService = cashbackService;
    this.withdrawalEventService = withdrawalEventService;
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

  public AdminPageResult<AdminInviteVO> pageInvites(Long inviterId, Long inviteeId, Boolean firstPaidOnly, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = inviteRelationMapper.countByAdminQuery(inviterId, inviteeId, firstPaidOnly);
    List<InviteRelation> records = inviteRelationMapper.findByAdminQuery(inviterId, inviteeId, firstPaidOnly, offset, safeSize);
    List<AdminInviteVO> result = records.stream().map(this::toAdminInviteVO).collect(Collectors.toList());
    return new AdminPageResult<>(result, total, safePage, safeSize);
  }

  public AdminPageResult<AdminOrderVO> pageOrders(String status, Long userId, String keyword, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = orderMapper.countByAdminQuery(status, userId, keyword);
    List<Order> records = orderMapper.findByAdminQuery(status, userId, keyword, offset, safeSize);
    List<AdminOrderVO> result = records.stream().map(this::toAdminOrderVO).collect(Collectors.toList());
    return new AdminPageResult<>(result, total, safePage, safeSize);
  }

  public byte[] exportOrdersCsv(String status, Long userId, String keyword) {
    StringBuilder sql = new StringBuilder()
        .append("SELECT o.id, o.order_no, o.user_id, COALESCE(u.nickname, '') AS user_nickname, ")
        .append("COALESCE(p.name, '') AS product_name, o.order_status, COALESCE(o.refund_status, 'NONE') AS refund_status, ")
        .append("COALESCE(o.pay_type, '') AS pay_type, COALESCE(o.transaction_id, '') AS transaction_id, ")
        .append("COALESCE(o.total_amount, 0) AS total_amount, o.created_at, o.pay_time, ")
        .append("COALESCE(o.tracking_no, '') AS tracking_no, COALESCE(o.recipient_name, '') AS recipient_name, ")
        .append("COALESCE(o.recipient_phone, '') AS recipient_phone, COALESCE(o.address, '') AS address ")
        .append("FROM orders o ")
        .append("LEFT JOIN users u ON u.id = o.user_id ")
        .append("LEFT JOIN products p ON p.id = o.product_id ")
        .append("WHERE 1=1");

    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" AND o.order_status = ?");
      args.add(status.trim());
    }
    if (userId != null) {
      sql.append(" AND o.user_id = ?");
      args.add(userId);
    }
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" AND (o.order_no LIKE ? OR o.tracking_no LIKE ? OR o.recipient_name LIKE ? OR o.recipient_phone LIKE ? ")
          .append("OR u.mobile LIKE ? OR u.nickname LIKE ? ")
          .append("OR EXISTS (SELECT 1 FROM user_wechat_auth uwa WHERE uwa.user_id = o.user_id ")
          .append("AND (uwa.nickname LIKE ? OR uwa.openid LIKE ?)))");
      String pattern = "%" + keyword.trim() + "%";
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
    }
    sql.append(" ORDER BY o.id DESC");

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
    StringBuilder csv = new StringBuilder();
    csv.append('\uFEFF');
    csv.append("订单ID,商户单号,用户ID,用户昵称,商品名称,订单状态,退款状态,支付方式,微信交易单号,订单金额,下单时间,支付时间,物流单号,收货人,收货手机号,收货地址\n");
    for (Map<String, Object> row : rows) {
      csv.append(csvCell(row.get("id"))).append(',')
          .append(csvCell(row.get("order_no"))).append(',')
          .append(csvCell(row.get("user_id"))).append(',')
          .append(csvCell(row.get("user_nickname"))).append(',')
          .append(csvCell(row.get("product_name"))).append(',')
          .append(csvCell(row.get("order_status"))).append(',')
          .append(csvCell(row.get("refund_status"))).append(',')
          .append(csvCell(row.get("pay_type"))).append(',')
          .append(csvCell(row.get("transaction_id"))).append(',')
          .append(csvCell(row.get("total_amount"))).append(',')
          .append(csvCell(formatCsvDateTime(row.get("created_at")))).append(',')
          .append(csvCell(formatCsvDateTime(row.get("pay_time")))).append(',')
          .append(csvCell(row.get("tracking_no"))).append(',')
          .append(csvCell(row.get("recipient_name"))).append(',')
          .append(csvCell(row.get("recipient_phone"))).append(',')
          .append(csvCell(row.get("address"))).append('\n');
    }
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  public AdminPageResult<AdminCashbackVO> pageCashbacks(Long userId, String type, String status, Integer page, Integer size) {
    int safePage = safePage(page);
    int safeSize = safeSize(size);
    int offset = (safePage - 1) * safeSize;
    long total = cashbackRecordMapper.countByAdminQuery(userId, type, status);
    List<CashbackRecord> records = cashbackRecordMapper.findByAdminQuery(userId, type, status, offset, safeSize);
    List<AdminCashbackVO> result = records.stream().map(this::toAdminCashbackVO).collect(Collectors.toList());
    return new AdminPageResult<>(result, total, safePage, safeSize);
  }

  public AdminPageResult<AdminWithdrawalRequestVO> enrichWithdrawalRequests(AdminPageResult<WithdrawalRequest> page) {
    List<AdminWithdrawalRequestVO> result = page.getRecords().stream()
        .map(this::toAdminWithdrawalRequestVO)
        .collect(Collectors.toList());
    return new AdminPageResult<>(result, page.getTotal(), page.getPage(), page.getSize());
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
    product.setDetailContent(normalizeNullableText(request.getDetailContent()));
    product.setImageUrl(request.getImageUrl());
    product.setActive(request.getActive());
    applyProductCashbackRules(product, request);
    boolean featured = resolveFeatured(request);
    product.setFeatured(featured);
    if (featured) {
      productMapper.clearFeatured();
    }
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
    current.setDetailContent(normalizeNullableText(request.getDetailContent()));
    current.setImageUrl(request.getImageUrl());
    current.setActive(request.getActive());
    applyProductCashbackRules(current, request);
    boolean featured = resolveFeatured(request);
    current.setFeatured(featured);
    if (featured) {
      productMapper.clearFeaturedExcept(productId);
    }
    productMapper.update(current);
    return productMapper.findById(productId);
  }

  @Transactional
  public void deleteProduct(Long productId) {
    Product current = productMapper.findById(productId);
    if (current == null) {
      throw new BusinessException("商品不存在");
    }
    long orderCount = orderMapper.countByProductId(productId);
    if (orderCount > 0) {
      throw new BusinessException("璇ュ晢鍝佸凡鍏宠仈鍘嗗彶璁㈠崟锛屼笉鍙垹闄わ紝璇蜂娇鐢ㄤ笅鏋舵柟寮?");
    }
    productMapper.deleteById(productId);
  }

  @Transactional
  public void updateUserStatus(Long userId, Integer status) {
    User user = userMapper.findById(userId);
    if (user == null) {
      throw new BusinessException("用户不存在");
    }
    if (status == null || (status != 0 && status != 1)) {
      throw new BusinessException("鐢ㄦ埛鐘舵€佸€奸敊璇?");
    }
    userMapper.updateStatus(userId, status);
    if (status == 0) {
      userSessionMapper.deleteByUserId(userId);
    }
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

  public Map<String, Object> getRefundPreview(Long orderId) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BusinessException("订单不存在");
    }

    List<CashbackRecord> affected = orderService.collectRefundAffectedCashbacks(order);
    BigDecimal deduction = orderService.calculateRefundCashbackDeduction(order);
    BigDecimal suggested = calculateActualRefundAmount(order.getTotalAmount(), BigDecimal.ZERO);
    if (deduction.compareTo(BigDecimal.ZERO) > 0) {
      suggested = calculateActualRefundAmount(order.getTotalAmount(), deduction);
    }

    List<Map<String, Object>> items = new ArrayList<>();
    for (CashbackRecord record : affected) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("cashbackId", record.getId());
      item.put("type", record.getType());
      item.put("amount", record.getAmount());
      item.put("status", record.getStatus());
      item.put("locked", cashbackService.isTransferLocked(record));
      item.put("remark", record.getRemark());
      items.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("orderId", orderId);
    result.put("orderAmount", order.getTotalAmount());
    result.put("cashbackDeduction", deduction);
    result.put("suggestedRefund", suggested);
    result.put("items", items);
    return result;
  }

  @Transactional
  public void refundOrder(Long orderId, String reason) {
    refundOrder(orderId, reason, null);
  }

  @Transactional
  public void refundOrder(Long orderId, String reason, BigDecimal customAmount) {
    Order order = validateOrderForRefund(orderId);
    String normalizedReason = reason == null || reason.isBlank() ? "协商退款" : reason.trim();
    executeRefund(order, normalizedReason, customAmount, LocalDateTime.now());
  }

  @Transactional
  public void approveRefundRequest(Long orderId, BigDecimal amount, String adminRemark, Long adminId) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BusinessException("订单不存在");
    }
    if (!"PENDING".equals(order.getRefundRequestStatus())) {
      throw new BusinessException("当前退款申请状态不是待审批，无法批准");
    }

    String remark = adminRemark == null || adminRemark.isBlank()
        ? (order.getRefundRequestReason() != null ? "用户申请: " + order.getRefundRequestReason() : "管理员批准退款")
        : adminRemark;
    String reviewRemark = remark.length() > 200 ? remark.substring(0, 200) : remark;

    int approved = orderMapper.approveRefundRequest(orderId, reviewRemark, adminId);
    if (approved <= 0) {
      throw new BusinessException("批准退款申请失败，请刷新后重试");
    }

    String refundReason = buildApprovalRefundReason(order, reviewRemark);
    executeRefund(order, refundReason, amount, LocalDateTime.now());
  }

  @Transactional
  public void rejectRefundRequest(Long orderId, String reviewRemark, Long adminId) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BusinessException("订单不存在");
    }
    if (!"PENDING".equals(order.getRefundRequestStatus())) {
      throw new BusinessException("当前退款申请状态不是待审批，无法驳回");
    }

    String remark = reviewRemark == null || reviewRemark.isBlank() ? "管理员驳回" : reviewRemark.trim();
    if (remark.length() > 200) {
      remark = remark.substring(0, 200);
    }

    int rejected = orderMapper.rejectRefundRequest(orderId, remark, adminId);
    if (rejected <= 0) {
      throw new BusinessException("驳回退款申请失败，请刷新后重试");
    }
  }

  private Order validateOrderForRefund(Long orderId) {
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
    return order;
  }

  private void executeRefund(Order order, String reason, BigDecimal customAmount, LocalDateTime refundApplyAt) {
    Long orderId = order.getId();
    BigDecimal refundAmount;
    String refundReason;
    if (customAmount != null) {
      if (customAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("退款金额必须大于0");
      }
      if (order.getTotalAmount() != null && customAmount.compareTo(order.getTotalAmount()) > 0) {
        throw new BusinessException("退款金额不能超过订单金额");
      }
      refundAmount = customAmount.setScale(2, RoundingMode.HALF_UP);
      refundReason = reason + " | admin custom refund " + refundAmount;
    } else {
      BigDecimal cashbackDeduction = orderService.calculateRefundCashbackDeduction(order);
      refundAmount = calculateActualRefundAmount(order.getTotalAmount(), cashbackDeduction);
      refundReason = appendCashbackDeductionReason(reason, cashbackDeduction, refundAmount);
    }

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

  private String buildApprovalRefundReason(Order order, String adminRemark) {
    String userReason = order.getRefundRequestReason() != null && !order.getRefundRequestReason().isBlank()
        ? "用户申请理由: " + order.getRefundRequestReason()
        : "";
    String admin = adminRemark != null && !adminRemark.isBlank() ? "管理员备注: " + adminRemark : "";
    if (!userReason.isEmpty() && !admin.isEmpty()) {
      return userReason + " | " + admin;
    }
    return !userReason.isEmpty() ? userReason : (!admin.isEmpty() ? admin : "管理员批准退款");
  }

  @Transactional
  public WithdrawalRequest approveWithdrawalRequest(Long requestId) {
    return approveWithdrawalRequest(requestId, null);
  }

  @Transactional
  public WithdrawalRequest approveWithdrawalRequest(Long requestId, BigDecimal customAmount) {
    WithdrawalRequest request = withdrawalRequestMapper.findById(requestId);
    if (request == null) {
      throw new BusinessException("提现申请不存在");
    }
    if (!"PENDING".equals(request.getStatus()) && !"WAITING_MATURITY".equals(request.getStatus())) {
      throw new BusinessException("提现申请当前状态为 " + withdrawalStatusText(request.getStatus()) + "，不可批准");
    }

    List<WithdrawalRequestItem> items = withdrawalRequestMapper.findItemsByRequestId(requestId);
    if (items.isEmpty()) {
      return cancelInvalidWithdrawalRequest(requestId, "提现申请已失效：没有关联的返现明细，已自动取消。");
    }

    String invalidReason = findInvalidWithdrawalReason(items);
    if (invalidReason != null) {
      return cancelInvalidWithdrawalRequest(requestId, invalidReason);
    }

    List<CashbackRecord> records = loadWithdrawalCashbackRecords(items);
    BigDecimal availableAmount = sumCashbackAmounts(records);
    BigDecimal approvedAmount = resolveApprovedWithdrawalAmount(records, customAmount);
    if (approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("没有已满 7 天的可批准返现金额，请输入自定义批准金额或等待返现到期");
    }
    if (approvedAmount.compareTo(availableAmount) > 0) {
      throw new BusinessException("批准金额不能超过提现申请金额");
    }

    List<WithdrawalRequestItem> approvedItems = rebuildWithdrawalItemsForApproval(requestId, records, approvedAmount);
    String approvalRemark = "Approved by admin | requested " + money(availableAmount) + " | approved " + money(approvedAmount);
    withdrawalRequestMapper.updateRequestSnapshot(requestId, approvedAmount, availableAmount, approvalRemark);
    withdrawalRequestMapper.updateStatus(requestId, "APPROVED", approvalRemark);
    try {
      for (WithdrawalRequestItem item : approvedItems) {
        transferCashback(item.getCashbackId());
      }
      WithdrawalRequest updated = withdrawalRequestMapper.findById(requestId);
      withdrawalEventService.publishStatusChanged(updated);
      return updated;
    } catch (RuntimeException ex) {
      withdrawalRequestMapper.updateStatus(requestId, "FAILED", trimReason(ex.getMessage()));
      withdrawalEventService.publishStatusChanged(withdrawalRequestMapper.findById(requestId));
      throw ex;
    }
  }

  private List<CashbackRecord> loadWithdrawalCashbackRecords(List<WithdrawalRequestItem> items) {
    List<CashbackRecord> records = new ArrayList<>();
    for (WithdrawalRequestItem item : items) {
      CashbackRecord record = cashbackRecordMapper.findById(item.getCashbackId());
      if (record == null) {
        throw new BusinessException("提现申请已失效：关联返现 #" + item.getCashbackId() + " 不存在，请刷新后重新处理");
      }
      if (!"PENDING".equals(record.getStatus())) {
        throw new BusinessException("提现申请已失效：关联返现 #" + record.getId()
            + " 当前状态为 " + cashbackStatusText(record.getStatus()) + "，只能批准待结算返现");
      }
      records.add(record);
    }
    records.sort(this::compareCashbackForApproval);
    return records;
  }

  private String findInvalidWithdrawalReason(List<WithdrawalRequestItem> items) {
    for (WithdrawalRequestItem item : items) {
      CashbackRecord record = cashbackRecordMapper.findById(item.getCashbackId());
      if (record == null) {
        return "提现申请已失效：关联返现 #" + item.getCashbackId() + " 不存在，已自动取消。";
      }
      if (!"PENDING".equals(record.getStatus())) {
        return "提现申请已失效：关联返现 #" + record.getId()
            + " 当前状态为 " + cashbackStatusText(record.getStatus())
            + "，只能批准待结算返现。已自动取消。";
      }
    }
    return null;
  }

  private WithdrawalRequest cancelInvalidWithdrawalRequest(Long requestId, String reason) {
    withdrawalRequestMapper.updateStatus(requestId, "CANCELLED", reason);
    WithdrawalRequest updated = withdrawalRequestMapper.findById(requestId);
    withdrawalEventService.publishStatusChanged(updated);
    return updated;
  }

  private String withdrawalStatusText(String status) {
    if (status == null || status.isBlank()) {
      return "-";
    }
    return switch (status) {
      case "PENDING" -> "待批准";
      case "WAITING_MATURITY" -> "待满 7 天";
      case "APPROVED" -> "已批准";
      case "COMPLETED" -> "已完成";
      case "FAILED" -> "失败";
      case "CANCELLED" -> "已取消";
      default -> status;
    };
  }

  private String cashbackStatusText(String status) {
    if (status == null || status.isBlank()) {
      return "-";
    }
    return switch (status) {
      case "PENDING" -> "待结算";
      case "WAIT_USER_CONFIRM" -> "待确认收款";
      case "PROCESSING" -> "打款中";
      case "TRANSFERING" -> "转账中";
      case "TRANSFERRED" -> "已到账";
      case "CANCELLED" -> "已取消";
      case "CANCELING" -> "撤销中";
      case "FAILED" -> "打款失败";
      default -> status;
    };
  }

  private BigDecimal resolveApprovedWithdrawalAmount(List<CashbackRecord> records, BigDecimal customAmount) {
    if (customAmount != null) {
      return money(customAmount);
    }
    BigDecimal maturedAmount = BigDecimal.ZERO;
    LocalDateTime now = LocalDateTime.now();
    for (CashbackRecord record : records) {
      if (isEligibleNow(record, now)) {
        maturedAmount = maturedAmount.add(money(record.getAmount()));
      }
    }
    return money(maturedAmount);
  }

  private BigDecimal sumCashbackAmounts(List<CashbackRecord> records) {
    BigDecimal amount = BigDecimal.ZERO;
    for (CashbackRecord record : records) {
      amount = amount.add(money(record.getAmount()));
    }
    return money(amount);
  }

  private List<WithdrawalRequestItem> rebuildWithdrawalItemsForApproval(
      Long requestId,
      List<CashbackRecord> records,
      BigDecimal approvedAmount
  ) {
    withdrawalRequestMapper.deleteItemsByRequestId(requestId);
    List<WithdrawalRequestItem> approvedItems = new ArrayList<>();
    BigDecimal remaining = money(approvedAmount);

    for (CashbackRecord record : records) {
      BigDecimal recordAmount = money(record.getAmount());
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        cashbackRecordMapper.clearWithdrawalRequestByCashbackId(record.getId());
        continue;
      }

      BigDecimal itemAmount = recordAmount.min(remaining);
      if (itemAmount.compareTo(recordAmount) < 0) {
        BigDecimal remainderAmount = money(recordAmount.subtract(itemAmount));
        String originalRemark = record.getRemark();
        cashbackRecordMapper.updateAmountAndRemark(
            record.getId(),
            itemAmount,
            appendRemark(originalRemark, "Partial withdrawal approved")
        );
        record.setAmount(itemAmount);
        CashbackRecord remainder = cloneCashbackRemainder(record, remainderAmount, originalRemark);
        cashbackRecordMapper.insert(remainder);
      }

      WithdrawalRequestItem newItem = new WithdrawalRequestItem();
      newItem.setRequestId(requestId);
      newItem.setCashbackId(record.getId());
      newItem.setAmount(itemAmount);
      withdrawalRequestMapper.insertItem(newItem);
      approvedItems.add(newItem);
      remaining = money(remaining.subtract(itemAmount));
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      throw new BusinessException("Approved amount cannot be allocated");
    }
    if (approvedItems.isEmpty()) {
      throw new BusinessException("No cashback items approved");
    }
    return approvedItems;
  }

  private CashbackRecord cloneCashbackRemainder(CashbackRecord source, BigDecimal amount, String originalRemark) {
    CashbackRecord record = new CashbackRecord();
    record.setUserId(source.getUserId());
    record.setOrderId(source.getOrderId());
    record.setType(source.getType());
    record.setAmount(money(amount));
    record.setBatchNo(source.getBatchNo());
    record.setStatus("PENDING");
    record.setRemark(appendRemark(originalRemark, "Partial withdrawal remainder"));
    record.setEligibleAt(source.getEligibleAt());
    record.setEarlyWithdrawal(source.getEarlyWithdrawal());
    return record;
  }

  private int compareCashbackForApproval(CashbackRecord left, CashbackRecord right) {
    LocalDateTime now = LocalDateTime.now();
    boolean leftEligible = isEligibleNow(left, now);
    boolean rightEligible = isEligibleNow(right, now);
    if (leftEligible != rightEligible) {
      return leftEligible ? -1 : 1;
    }
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

  private boolean isEligibleNow(CashbackRecord record, LocalDateTime now) {
    return record == null || record.getEligibleAt() == null || !record.getEligibleAt().isAfter(now);
  }

  private BigDecimal money(BigDecimal amount) {
    if (amount == null) {
      return BigDecimal.ZERO;
    }
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  private String appendRemark(String current, String suffix) {
    if (suffix == null || suffix.isBlank()) {
      return current;
    }
    if (current == null || current.isBlank()) {
      return suffix.trim();
    }
    return current + " | " + suffix.trim();
  }

  public CashbackRecord transferCashback(Long cashbackId) {
    CashbackRecord cb = cashbackRecordMapper.findById(cashbackId);
    if (cb == null) {
      throw new BusinessException("返现记录不存在");
    }
    if (!"PENDING".equals(cb.getStatus()) && !isRetryableTransferConfigFailure(cb)) {
      throw new BusinessException("该笔佣金不处于待发状态");
    }
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
    int inviteProductCleared = inviteProductRelationMapper.clearAllFirstPaid();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("userUpdated", userUpdated);
    result.put("cashbackCancelled", cashbackCancelled);
    result.put("inviteCleared", inviteCleared);
    result.put("inviteProductCleared", inviteProductCleared);
    return result;
  }

  @Transactional
  public Map<String, Object> resetAllOrders() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("withdrawalRequestItems", jdbcTemplate.update("DELETE FROM withdrawal_request_items"));
    result.put("withdrawalRequests", jdbcTemplate.update("DELETE FROM withdrawal_requests"));
    result.put("shippingRecords", jdbcTemplate.update("DELETE FROM shipping_records"));
    result.put("cashbackDebts", jdbcTemplate.update("DELETE FROM cashback_debts"));
    result.put("cashbacks", jdbcTemplate.update("DELETE FROM cashback_records"));
    result.put("inviteFirstPaidCleared", inviteRelationMapper.clearAllFirstPaid());
    result.put("inviteProductRelations", jdbcTemplate.update("DELETE FROM invite_product_relations"));
    result.put("productSalesReset", jdbcTemplate.update("UPDATE products SET sales_count = 0"));
    result.put("orders", jdbcTemplate.update("DELETE FROM orders"));
    result.put("userCashbackResetCleared", jdbcTemplate.update("UPDATE users SET cashback_reset_at = NULL WHERE cashback_reset_at IS NOT NULL"));
    return result;
  }

  @Transactional
  public Map<String, Object> resetAllUsers() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("smsCodes", jdbcTemplate.update("DELETE FROM sms_login_codes"));
    result.put("addresses", jdbcTemplate.update("DELETE FROM user_addresses"));
    result.put("sessions", jdbcTemplate.update("DELETE FROM user_sessions"));
    result.put("wechatAuths", jdbcTemplate.update("DELETE FROM user_wechat_auth"));
    result.put("inviteProductRelations", jdbcTemplate.update("DELETE FROM invite_product_relations"));
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

  private String csvCell(Object value) {
    String text = value == null ? "" : String.valueOf(value);
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }

  private String formatCsvDateTime(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toLocalDateTime().format(CSV_TIME_FORMATTER);
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.format(CSV_TIME_FORMATTER);
    }
    return String.valueOf(value);
  }

  private boolean resolveFeatured(AdminProductUpsertRequest request) {
    return Boolean.TRUE.equals(request.getFeatured()) && Boolean.TRUE.equals(request.getActive());
  }

  private void applyProductCashbackRules(Product product, AdminProductUpsertRequest request) {
    product.setPersonalSecondRatio(request.getPersonalSecondRatio());
    product.setPersonalThirdRatio(request.getPersonalThirdRatio());
    product.setPersonalFourthRatio(request.getPersonalFourthRatio());
    product.setInviteBatchSize(request.getInviteBatchSize());
    product.setInviteFirstRatio(request.getInviteFirstRatio());
    product.setInviteRepeatRatio(request.getInviteRepeatRatio());
  }

  private AdminUserVO toAdminUserVO(User user) {
    AdminUserVO vo = new AdminUserVO();
    vo.setId(user.getId());
    vo.setPhone(user.getPhone());
    vo.setNickname(user.getNickname());
    vo.setAvatarUrl(resolveWechatAvatarUrl(user.getId()));
    vo.setInviteCode(user.getInviteCode());
    vo.setInviterId(user.getInviterId());
    vo.setStatus(user.getStatus());
    vo.setWechatWebOpenid(resolveWechatWebOpenid(user.getId()));
    vo.setWechatMiniappOpenid(resolveWechatMiniappOpenid(user.getId()));
    vo.setCreatedAt(user.getCreatedAt());
    return vo;
  }

  private AdminInviteVO toAdminInviteVO(InviteRelation relation) {
    AdminInviteVO vo = new AdminInviteVO();
    vo.setId(relation.getId());
    vo.setInviteNo(formatInviteNo(relation.getId()));
    vo.setInviterId(relation.getInviterId());
    vo.setInviteeId(relation.getInviteeId());
    vo.setBoundAt(relation.getBoundAt());
    vo.setFirstPaidAt(relation.getFirstPaidAt());

    User inviter = relation.getInviterId() == null ? null : userMapper.findById(relation.getInviterId());
    User invitee = relation.getInviteeId() == null ? null : userMapper.findById(relation.getInviteeId());
    vo.setInviterNickname(resolveAdminNickname(inviter));
    vo.setInviterAvatarUrl(resolveWechatAvatarUrl(relation.getInviterId()));
    vo.setInviteeNickname(resolveAdminNickname(invitee));
    vo.setInviteeAvatarUrl(resolveWechatAvatarUrl(relation.getInviteeId()));
    return vo;
  }

  private AdminOrderVO toAdminOrderVO(Order order) {
    AdminOrderVO vo = new AdminOrderVO(order);
    Long userId = order.getUserId();
    User user = userId == null ? null : userMapper.findById(userId);
    vo.setUserNickname(resolveAdminNickname(user));
    vo.setUserAvatarUrl(resolveWechatAvatarUrl(userId));
    return vo;
  }

  private AdminCashbackVO toAdminCashbackVO(CashbackRecord record) {
    AdminCashbackVO vo = new AdminCashbackVO(record);
    Long userId = record.getUserId();
    User user = userId == null ? null : userMapper.findById(userId);
    vo.setUserNickname(resolveAdminNickname(user));
    vo.setUserAvatarUrl(resolveWechatAvatarUrl(userId));
    return vo;
  }

  private AdminWithdrawalRequestVO toAdminWithdrawalRequestVO(WithdrawalRequest request) {
    AdminWithdrawalRequestVO vo = new AdminWithdrawalRequestVO(request);
    Long userId = request.getUserId();
    User user = userId == null ? null : userMapper.findById(userId);
    vo.setUserNickname(resolveAdminNickname(user));
    vo.setUserAvatarUrl(resolveWechatAvatarUrl(userId));
    return vo;
  }

  private String formatInviteNo(Long id) {
    if (id == null) {
      return "INV-000000";
    }
    return String.format(Locale.ROOT, "INV-%06d", id);
  }

  private String resolveAdminNickname(User user) {
    if (user == null) {
      return "";
    }
    String userNickname = trimToEmpty(user.getNickname());
    if (!userNickname.isBlank()) {
      return userNickname;
    }
    return resolveWechatNickname(user.getId());
  }

  private String resolveWechatNickname(Long userId) {
    if (userId == null) {
      return "";
    }
    UserWechatAuth miniappAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_MINIAPP);
    String miniappNickname = authNickname(miniappAuth);
    if (!miniappNickname.isBlank()) {
      return miniappNickname;
    }

    UserWechatAuth webAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_WEB);
    String webNickname = authNickname(webAuth);
    if (!webNickname.isBlank()) {
      return webNickname;
    }

    UserWechatAuth legacyAuth = userWechatAuthMapper.findByUserId(userId);
    return authNickname(legacyAuth);
  }

  private String resolveWechatAvatarUrl(Long userId) {
    if (userId == null) {
      return "";
    }
    UserWechatAuth miniappAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_MINIAPP);
    String miniappAvatar = authAvatarUrl(miniappAuth);
    if (!miniappAvatar.isBlank()) {
      return miniappAvatar;
    }

    UserWechatAuth webAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_WEB);
    String webAvatar = authAvatarUrl(webAuth);
    if (!webAvatar.isBlank()) {
      return webAvatar;
    }

    UserWechatAuth legacyAuth = userWechatAuthMapper.findByUserId(userId);
    return authAvatarUrl(legacyAuth);
  }

  private String authNickname(UserWechatAuth auth) {
    return auth == null ? "" : trimToEmpty(auth.getNickname());
  }

  private String authAvatarUrl(UserWechatAuth auth) {
    return auth == null ? "" : trimToEmpty(auth.getAvatarUrl());
  }

  private String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
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

  private String normalizeNullableText(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
