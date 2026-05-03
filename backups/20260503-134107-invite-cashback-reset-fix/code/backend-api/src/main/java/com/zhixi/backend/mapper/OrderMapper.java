package com.zhixi.backend.mapper;

import com.zhixi.backend.model.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
  String ORDER_SELECT_COLUMNS =
      "o.id,o.order_no AS orderNo,o.user_id AS userId,o.product_id AS productId,"
          + "p.name AS productName,p.main_image_url AS productImageUrl,"
          + "o.quantity,o.product_amount AS productAmount,o.shipping_fee AS shippingFee,o.cashback_base_amount AS cashbackBaseAmount,"
          + "o.total_amount AS totalAmount,o.order_status AS status,"
          + "o.recipient_name AS recipientName,o.recipient_phone AS recipientPhone,o.province,o.city,o.district,o.address,o.tracking_no AS trackingNo,"
          + "o.pay_type AS payType,o.transaction_id AS transactionId,o.refund_status AS refundStatus,o.refund_no AS refundNo,o.refund_id AS refundId,o.refund_apply_time AS refundApplyAt,o.refund_complete_time AS refundCompletedAt,"
          + "o.refund_request_status AS refundRequestStatus,o.refund_request_reason AS refundRequestReason,o.refund_request_at AS refundRequestAt,"
          + "o.refund_review_at AS refundReviewAt,o.refund_review_remark AS refundReviewRemark,o.refund_review_admin_id AS refundReviewAdminId,"
          + "o.created_at AS createdAt,o.pay_time AS paidAt,o.complete_time AS completedAt ";

  @Insert("INSERT INTO orders("
      + "order_no,user_id,product_id,quantity,recipient_name,recipient_phone,province,city,district,address,"
      + "order_status,product_amount,shipping_fee,cashback_base_amount,total_amount,pay_amount,remark,created_at,updated_at"
      + ") VALUES ("
      + "CONCAT('ZX', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), LPAD(FLOOR(RAND()*10000),4,'0')),"
      + "#{userId},#{productId},#{quantity},#{recipientName},#{recipientPhone},#{province},#{city},#{district},#{address},"
      + "#{status},#{productAmount},#{shippingFee},#{cashbackBaseAmount},#{totalAmount},#{totalAmount},'',NOW(),NOW()"
      + ")")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Order order);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.id = #{id}")
  Order findById(Long id);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.order_no = #{orderNo} LIMIT 1")
  Order findByOrderNo(String orderNo);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.refund_no = #{refundNo} LIMIT 1")
  Order findByRefundNo(String refundNo);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.user_id = #{userId} ORDER BY o.id DESC")
  List<Order> findByUserId(Long userId);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id ORDER BY o.id DESC")
  List<Order> findAll();

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM orders o",
      "LEFT JOIN users u ON u.id = o.user_id",
      "WHERE 1=1",
      "<if test='status != null and status != \"\"'>",
      "  AND o.order_status = #{status}",
      "</if>",
      "<if test='userId != null'>",
      "  AND o.user_id = #{userId}",
      "</if>",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND (o.order_no LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.tracking_no LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.recipient_name LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.recipient_phone LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.mobile LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.nickname LIKE CONCAT('%', #{keyword}, '%')",
      "       OR EXISTS (",
      "         SELECT 1 FROM user_wechat_auth uwa",
      "         WHERE uwa.user_id = o.user_id",
      "           AND (uwa.nickname LIKE CONCAT('%', #{keyword}, '%') OR uwa.openid LIKE CONCAT('%', #{keyword}, '%'))",
      "       ))",
      "</if>",
      "</script>"
  })
  long countByAdminQuery(
      @Param("status") String status,
      @Param("userId") Long userId,
      @Param("keyword") String keyword
  );

  @Select({
      "<script>",
      "SELECT ", ORDER_SELECT_COLUMNS,
      "FROM orders o LEFT JOIN products p ON p.id = o.product_id LEFT JOIN users u ON u.id = o.user_id",
      "WHERE 1=1",
      "<if test='status != null and status != \"\"'>",
      "  AND o.order_status = #{status}",
      "</if>",
      "<if test='userId != null'>",
      "  AND o.user_id = #{userId}",
      "</if>",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND (o.order_no LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.tracking_no LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.recipient_name LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.recipient_phone LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.mobile LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.nickname LIKE CONCAT('%', #{keyword}, '%')",
      "       OR EXISTS (",
      "         SELECT 1 FROM user_wechat_auth uwa",
      "         WHERE uwa.user_id = o.user_id",
      "           AND (uwa.nickname LIKE CONCAT('%', #{keyword}, '%') OR uwa.openid LIKE CONCAT('%', #{keyword}, '%'))",
      "       ))",
      "</if>",
      "ORDER BY o.id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<Order> findByAdminQuery(
      @Param("status") String status,
      @Param("userId") Long userId,
      @Param("keyword") String keyword,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Select("SELECT COUNT(1) FROM orders WHERE user_id = #{userId} AND order_status IN ('PAID','SHIPPED','COMPLETED')")
  int countPaidByUser(Long userId);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.user_id = #{userId} AND o.order_status IN ('PAID','SHIPPED','COMPLETED') ORDER BY o.id ASC")
  List<Order> findValidPaidByUserOrderByIdAsc(Long userId);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id "
      + "WHERE o.user_id = #{userId} AND o.product_id = #{productId} AND o.order_status IN ('PAID','SHIPPED','COMPLETED') ORDER BY o.id ASC")
  List<Order> findValidPaidByUserAndProductOrderByIdAsc(@Param("userId") Long userId, @Param("productId") Long productId);

  @Select("SELECT " + ORDER_SELECT_COLUMNS + "FROM orders o LEFT JOIN products p ON p.id = o.product_id "
      + "WHERE o.user_id = #{userId} AND o.product_id = #{productId} AND o.order_status IN ('PAID','SHIPPED','COMPLETED') ORDER BY o.id ASC LIMIT 1")
  Order findFirstValidPaidByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM orders o",
      "WHERE o.user_id = #{userId}",
      "  AND o.order_status IN ('PAID','SHIPPED','COMPLETED')",
      "  AND o.id <![CDATA[ < ]]> #{orderId}",
      "<if test='resetAt != null'>",
      "  AND o.pay_time <![CDATA[ > ]]> #{resetAt}",
      "</if>",
      "</script>"
  })
  int countPaidBeforeOrder(@Param("userId") Long userId, @Param("orderId") Long orderId, @Param("resetAt") java.time.LocalDateTime resetAt);

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM orders o",
      "WHERE o.user_id = #{userId}",
      "  AND o.product_id = #{productId}",
      "  AND o.order_status IN ('PAID','SHIPPED','COMPLETED')",
      "  AND o.id <![CDATA[ < ]]> #{orderId}",
      "<if test='resetAt != null'>",
      "  AND o.pay_time <![CDATA[ > ]]> #{resetAt}",
      "</if>",
      "</script>"
  })
  int countPaidBeforeOrderByProduct(
      @Param("userId") Long userId,
      @Param("productId") Long productId,
      @Param("orderId") Long orderId,
      @Param("resetAt") java.time.LocalDateTime resetAt
  );

  @Update("UPDATE orders SET order_status = 'PAID', pay_time = #{paidAt}, pay_type = #{payType}, transaction_id = #{transactionId}, updated_at = NOW() WHERE id = #{id} AND order_status = 'PENDING'")
  int markPaid(@Param("id") Long id, @Param("paidAt") LocalDateTime paidAt, @Param("payType") String payType, @Param("transactionId") String transactionId);

  @Update("UPDATE orders SET order_status = 'COMPLETED', complete_time = #{completedAt}, updated_at = NOW() WHERE id = #{id} AND order_status IN ('PAID','SHIPPED')")
  int markCompleted(@Param("id") Long id, @Param("completedAt") LocalDateTime completedAt);

  @Update("UPDATE orders SET refund_status = #{status}, refund_no = #{refundNo}, refund_id = #{refundId}, refund_apply_time = #{refundApplyAt}, updated_at = NOW() WHERE id = #{id}")
  int updateRefundRequest(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("refundNo") String refundNo,
      @Param("refundId") String refundId,
      @Param("refundApplyAt") LocalDateTime refundApplyAt
  );

  @Update("UPDATE orders SET order_status = 'REFUNDED', refund_complete_time = NOW(), updated_at = NOW() WHERE id = #{id} AND order_status <> 'REFUNDED'")
  int markRefunded(@Param("id") Long id);

  @Update("UPDATE orders SET order_status = 'REFUNDED', refund_status = 'SUCCESS', refund_id = COALESCE(#{refundId}, refund_id), updated_at = NOW() WHERE refund_no = #{refundNo}")
  int markRefundSuccessByRefundNo(@Param("refundNo") String refundNo, @Param("refundId") String refundId);

  @Update("UPDATE orders SET refund_status = 'FAILED', refund_id = COALESCE(#{refundId}, refund_id), updated_at = NOW() WHERE refund_no = #{refundNo}")
  int markRefundFailedByRefundNo(@Param("refundNo") String refundNo, @Param("refundId") String refundId);

  @Update("UPDATE orders SET refund_request_status = 'PENDING', refund_request_reason = #{reason}, refund_request_at = NOW(),"
      + " refund_review_at = NULL, refund_review_remark = NULL, refund_review_admin_id = NULL, updated_at = NOW()"
      + " WHERE id = #{id} AND refund_request_status IN ('NONE','REJECTED')")
  int insertRefundRequest(@Param("id") Long id, @Param("reason") String reason);

  @Update("UPDATE orders SET refund_request_status = 'APPROVED', refund_review_at = NOW(),"
      + " refund_review_remark = #{reviewRemark}, refund_review_admin_id = #{adminId}, updated_at = NOW()"
      + " WHERE id = #{id} AND refund_request_status = 'PENDING'")
  int approveRefundRequest(@Param("id") Long id, @Param("reviewRemark") String reviewRemark, @Param("adminId") Long adminId);

  @Update("UPDATE orders SET refund_request_status = 'REJECTED', refund_review_at = NOW(),"
      + " refund_review_remark = #{reviewRemark}, refund_review_admin_id = #{adminId}, updated_at = NOW()"
      + " WHERE id = #{id} AND refund_request_status = 'PENDING'")
  int rejectRefundRequest(@Param("id") Long id, @Param("reviewRemark") String reviewRemark, @Param("adminId") Long adminId);

  @Select(
      "SELECT COALESCE(SUM(COALESCE(pay_amount, total_amount)),0) FROM orders "
          + "WHERE order_status IN ('PAID','SHIPPED','COMPLETED')"
  )
  Double totalPaidAmount();

  @Select(
      "SELECT COALESCE(SUM(COALESCE(pay_amount, total_amount)),0) FROM orders "
          + "WHERE order_status IN ('PAID','SHIPPED','COMPLETED') "
          + "AND DATE(pay_time) = CURRENT_DATE"
  )
  Double todayPaidAmount();

  @Select("SELECT COUNT(1) FROM orders WHERE DATE(created_at) = CURRENT_DATE")
  long countTodayOrders();

  @Select("SELECT COUNT(1) FROM orders WHERE order_status = 'PAID'")
  long countPendingShipments();

  @Select("SELECT COUNT(1) FROM orders WHERE product_id = #{productId}")
  long countByProductId(@Param("productId") Long productId);

  @Update("UPDATE orders SET order_status = 'CANCELLED', cancel_time = NOW(), updated_at = NOW() WHERE order_status = 'PENDING' AND created_at < NOW() - INTERVAL 15 MINUTE")
  int cancelExpiredOrders();

  @Update("UPDATE orders SET order_status = 'SHIPPED', tracking_no = #{trackingNo}, updated_at = NOW() WHERE id = #{id} AND order_status = 'PAID'")
  int markShipped(@Param("id") Long id, @Param("trackingNo") String trackingNo);

  @Update("UPDATE orders SET order_status = 'PAID', tracking_no = NULL, updated_at = NOW() WHERE id = #{id} AND order_status = 'SHIPPED'")
  int markUnshipped(@Param("id") Long id);
}
