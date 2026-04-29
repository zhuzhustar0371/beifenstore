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
      "o.id,o.order_no AS orderNo,o.user_id AS userId,o.product_id AS productId," +
      "p.name AS productName,p.main_image_url AS productImageUrl," +
      "o.quantity,o.total_amount AS totalAmount,o.order_status AS status," +
      "o.recipient_name AS recipientName,o.recipient_phone AS recipientPhone,o.address,o.tracking_no AS trackingNo," +
      "o.pay_type AS payType,o.transaction_id AS transactionId,o.refund_status AS refundStatus,o.refund_no AS refundNo,o.refund_id AS refundId,o.refund_apply_time AS refundApplyAt," +
      "o.created_at AS createdAt,o.pay_time AS paidAt,o.complete_time AS completedAt ";

  @Insert("INSERT INTO orders(" +
      "order_no,user_id,product_id,quantity,recipient_name,recipient_phone,address," +
      "order_status,total_amount,pay_amount,remark,created_at,updated_at" +
      ") VALUES (" +
      "CONCAT('ZX', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), LPAD(FLOOR(RAND()*10000),4,'0'))," +
      "#{userId},#{productId},#{quantity},#{recipientName},#{recipientPhone},#{address}," +
      "#{status},#{totalAmount},#{totalAmount},'',NOW(),NOW()" +
      ")")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Order order);

  @Select("SELECT " + ORDER_SELECT_COLUMNS +
      "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.id = #{id}")
  Order findById(Long id);

  @Select("SELECT " + ORDER_SELECT_COLUMNS +
      "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.order_no = #{orderNo} LIMIT 1")
  Order findByOrderNo(String orderNo);

  @Select("SELECT " + ORDER_SELECT_COLUMNS +
      "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.refund_no = #{refundNo} LIMIT 1")
  Order findByRefundNo(String refundNo);

  @Select("SELECT " + ORDER_SELECT_COLUMNS +
      "FROM orders o LEFT JOIN products p ON p.id = o.product_id WHERE o.user_id = #{userId} ORDER BY o.id DESC")
  List<Order> findByUserId(Long userId);

  @Select("SELECT " + ORDER_SELECT_COLUMNS +
      "FROM orders o LEFT JOIN products p ON p.id = o.product_id ORDER BY o.id DESC")
  List<Order> findAll();

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM orders o",
      "WHERE 1=1",
      "<if test='status != null and status != \"\"'>",
      "  AND o.order_status = #{status}",
      "</if>",
      "<if test='userId != null'>",
      "  AND o.user_id = #{userId}",
      "</if>",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND (o.order_no LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.tracking_no LIKE CONCAT('%', #{keyword}, '%'))",
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
      "FROM orders o LEFT JOIN products p ON p.id = o.product_id",
      "WHERE 1=1",
      "<if test='status != null and status != \"\"'>",
      "  AND o.order_status = #{status}",
      "</if>",
      "<if test='userId != null'>",
      "  AND o.user_id = #{userId}",
      "</if>",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND (o.order_no LIKE CONCAT('%', #{keyword}, '%')",
      "       OR o.tracking_no LIKE CONCAT('%', #{keyword}, '%'))",
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

  @Update("UPDATE orders SET order_status = 'PAID', pay_time = #{paidAt}, pay_type = #{payType}, transaction_id = #{transactionId}, updated_at = NOW() WHERE id = #{id} AND order_status = 'PENDING'")
  int markPaid(@Param("id") Long id, @Param("paidAt") LocalDateTime paidAt, @Param("payType") String payType, @Param("transactionId") String transactionId);

  @Update("UPDATE orders SET refund_status = #{status}, refund_no = #{refundNo}, refund_id = #{refundId}, refund_apply_time = #{refundApplyAt}, updated_at = NOW() WHERE id = #{id}")
  int updateRefundRequest(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("refundNo") String refundNo,
      @Param("refundId") String refundId,
      @Param("refundApplyAt") LocalDateTime refundApplyAt
  );

  @Update("UPDATE orders SET order_status = 'REFUNDED', updated_at = NOW() WHERE id = #{id} AND order_status <> 'REFUNDED'")
  int markRefunded(@Param("id") Long id);

  @Update("UPDATE orders SET order_status = 'REFUNDED', refund_status = 'SUCCESS', refund_id = COALESCE(#{refundId}, refund_id), updated_at = NOW() WHERE refund_no = #{refundNo}")
  int markRefundSuccessByRefundNo(@Param("refundNo") String refundNo, @Param("refundId") String refundId);

  @Update("UPDATE orders SET refund_status = 'FAILED', refund_id = COALESCE(#{refundId}, refund_id), updated_at = NOW() WHERE refund_no = #{refundNo}")
  int markRefundFailedByRefundNo(@Param("refundNo") String refundNo, @Param("refundId") String refundId);

  @Select("SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE order_status IN ('PAID','SHIPPED','COMPLETED')")
  Double totalPaidAmount();

  @Select("SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE order_status IN ('PAID','SHIPPED','COMPLETED') AND DATE(pay_time) = CURRENT_DATE")
  Double todayPaidAmount();

  @Select("SELECT COUNT(1) FROM orders WHERE DATE(created_at) = CURRENT_DATE")
  long countTodayOrders();

  @Select("SELECT COUNT(1) FROM orders WHERE order_status = 'PAID'")
  long countPendingShipments();

  @Update("UPDATE orders SET order_status = 'SHIPPED', tracking_no = #{trackingNo}, updated_at = NOW() WHERE id = #{id} AND order_status IN ('PAID','PENDING')")
  int markShipped(@Param("id") Long id, @Param("trackingNo") String trackingNo);
}
