package com.zhixi.backend.mapper;

import com.zhixi.backend.model.CashbackRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CashbackRecordMapper {
  String CASHBACK_COLUMNS =
      "id,user_id AS userId,related_order_id AS orderId,product_id AS productId,cashback_type AS type,amount,"
          + "related_invite_batch_id AS batchNo,status,remark,out_batch_no AS outBatchNo,out_detail_no AS outDetailNo,"
          + "transfer_id AS transferId,transfer_detail_id AS transferDetailId,transfer_fail_reason AS transferFailReason,"
          + "transfer_package_info AS transferPackageInfo,transfer_time AS transferTime,eligible_at AS eligibleAt,"
          + "withdrawal_request_id AS withdrawalRequestId,early_withdrawal AS earlyWithdrawal,created_at AS createdAt ";

  @Insert("INSERT INTO cashback_records(user_id,related_order_id,product_id,cashback_type,amount,related_invite_batch_id,status,remark,out_batch_no,out_detail_no,eligible_at,early_withdrawal,created_at) "
      + "VALUES(#{userId},#{orderId},#{productId},#{type},#{amount},#{batchNo},#{status},#{remark},#{outBatchNo},#{outDetailNo},#{eligibleAt},COALESCE(#{earlyWithdrawal},0),NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(CashbackRecord record);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records WHERE id = #{id}")
  CashbackRecord findById(Long id);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records WHERE out_detail_no = #{outDetailNo} ORDER BY id DESC LIMIT 1")
  CashbackRecord findByOutDetailNo(String outDetailNo);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records WHERE user_id = #{userId} ORDER BY id DESC")
  List<CashbackRecord> findByUserId(Long userId);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records WHERE related_order_id = #{orderId} ORDER BY id DESC")
  List<CashbackRecord> findByOrderId(Long orderId);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records ORDER BY id DESC")
  List<CashbackRecord> findAll();

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records "
      + "WHERE user_id = #{userId} AND cashback_type = 'INVITE_BATCH' ORDER BY related_invite_batch_id DESC, id DESC")
  List<CashbackRecord> findInviteBatchByUserId(Long userId);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records "
      + "WHERE user_id = #{userId} AND product_id = #{productId} AND cashback_type = 'INVITE_BATCH' "
      + "ORDER BY related_invite_batch_id DESC, id DESC")
  List<CashbackRecord> findInviteBatchByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM cashback_records cr",
      "LEFT JOIN users u ON u.id = cr.user_id",
      "LEFT JOIN user_wechat_auth uwa ON uwa.user_id = u.id",
      "WHERE 1=1",
      "<if test='userId != null'>",
      "  AND cr.user_id = #{userId}",
      "</if>",
      "<if test='type != null and type != \"\"'>",
      "  AND cr.cashback_type = #{type}",
      "</if>",
      "<if test='status != null and status != \"\"'>",
      "  AND cr.status = #{status}",
      "</if>",
      "<if test='nickname != null and nickname != \"\"'>",
      "  AND (u.nickname LIKE CONCAT('%', #{nickname}, '%') OR uwa.nickname LIKE CONCAT('%', #{nickname}, '%'))",
      "</if>",
      "</script>"
  })
  long countByAdminQuery(
      @Param("userId") Long userId,
      @Param("type") String type,
      @Param("status") String status,
      @Param("nickname") String nickname
  );

  @Select({
      "<script>",
      "SELECT cr.id, cr.user_id AS userId, cr.related_order_id AS orderId, cr.product_id AS productId,",
      "cr.cashback_type AS type, cr.amount, cr.related_invite_batch_id AS batchNo, cr.status,",
      "cr.remark, cr.out_batch_no AS outBatchNo, cr.out_detail_no AS outDetailNo,",
      "cr.transfer_id AS transferId, cr.transfer_detail_id AS transferDetailId, cr.transfer_fail_reason AS transferFailReason,",
      "cr.transfer_package_info AS transferPackageInfo, cr.transfer_time AS transferTime, cr.eligible_at AS eligibleAt,",
      "cr.withdrawal_request_id AS withdrawalRequestId, cr.early_withdrawal AS earlyWithdrawal, cr.created_at AS createdAt ",
      "FROM cashback_records cr",
      "LEFT JOIN users u ON u.id = cr.user_id",
      "LEFT JOIN user_wechat_auth uwa ON uwa.user_id = u.id",
      "WHERE 1=1",
      "<if test='userId != null'>",
      "  AND cr.user_id = #{userId}",
      "</if>",
      "<if test='type != null and type != \"\"'>",
      "  AND cr.cashback_type = #{type}",
      "</if>",
      "<if test='status != null and status != \"\"'>",
      "  AND cr.status = #{status}",
      "</if>",
      "<if test='nickname != null and nickname != \"\"'>",
      "  AND (u.nickname LIKE CONCAT('%', #{nickname}, '%') OR uwa.nickname LIKE CONCAT('%', #{nickname}, '%'))",
      "</if>",
      "ORDER BY cr.id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<CashbackRecord> findByAdminQuery(
      @Param("userId") Long userId,
      @Param("type") String type,
      @Param("status") String status,
      @Param("nickname") String nickname,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Select("SELECT COUNT(1) FROM cashback_records "
      + "WHERE user_id = #{userId} AND product_id = #{productId} AND cashback_type = 'INVITE_BATCH' "
      + "AND related_invite_batch_id = #{batchNo} AND status <> 'CANCELLED'")
  int existsInviteBatch(@Param("userId") Long userId, @Param("productId") Long productId, @Param("batchNo") Integer batchNo);

  @Update("UPDATE cashback_records SET status = 'PROCESSING', out_batch_no = #{outBatchNo}, out_detail_no = #{outDetailNo}, transfer_fail_reason = NULL, transfer_package_info = NULL, transfer_time = #{transferTime} WHERE id = #{id} AND status = #{currentStatus}")
  int markTransferProcessing(
      @Param("id") Long id,
      @Param("currentStatus") String currentStatus,
      @Param("outBatchNo") String outBatchNo,
      @Param("outDetailNo") String outDetailNo,
      @Param("transferTime") java.time.LocalDateTime transferTime
  );

  @Update("UPDATE cashback_records SET status = #{status}, out_batch_no = #{outBatchNo}, out_detail_no = #{outDetailNo}, transfer_id = #{transferId}, transfer_detail_id = #{transferDetailId}, transfer_fail_reason = #{transferFailReason}, transfer_package_info = #{transferPackageInfo}, transfer_time = #{transferTime} WHERE id = #{id}")
  int updateTransferResult(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("outBatchNo") String outBatchNo,
      @Param("outDetailNo") String outDetailNo,
      @Param("transferId") String transferId,
      @Param("transferDetailId") String transferDetailId,
      @Param("transferFailReason") String transferFailReason,
      @Param("transferPackageInfo") String transferPackageInfo,
      @Param("transferTime") java.time.LocalDateTime transferTime
  );

  @Update("UPDATE cashback_records SET status = #{status}, remark = #{remark} WHERE id = #{id}")
  int updateStatusAndRemark(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("remark") String remark
  );

  @Update("UPDATE cashback_records SET status = 'PENDING', out_batch_no = NULL, out_detail_no = NULL, transfer_id = NULL, transfer_detail_id = NULL, transfer_fail_reason = NULL, transfer_package_info = NULL WHERE id = #{id} AND status = 'FAILED'")
  int resetFailedTransferForRetry(@Param("id") Long id);

  @Update("UPDATE cashback_records SET status = 'CANCELLED', remark = CONCAT(COALESCE(remark, ''), ' | 鍏ㄥ眬閲嶇疆缁熻') WHERE status = 'PENDING'")
  int cancelAllPending();

  @Update("UPDATE cashback_records SET early_withdrawal = 1 WHERE id = #{id}")
  int markEarlyWithdrawal(@Param("id") Long id);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records "
      + "WHERE user_id = #{userId} AND status = 'PENDING' AND withdrawal_request_id IS NULL ORDER BY COALESCE(eligible_at, created_at) ASC, id ASC")
  List<CashbackRecord> findPendingUnrequestedByUserId(Long userId);

  @Select("SELECT " + CASHBACK_COLUMNS + "FROM cashback_records "
      + "WHERE user_id = #{userId} AND status = 'PENDING' AND withdrawal_request_id IS NULL AND (eligible_at IS NULL OR eligible_at <= NOW()) ORDER BY id ASC")
  List<CashbackRecord> findWithdrawableByUserId(Long userId);

  @Update("UPDATE cashback_records SET withdrawal_request_id = #{requestId} WHERE id = #{cashbackId} AND status = 'PENDING' AND withdrawal_request_id IS NULL")
  int markWithdrawalRequested(@Param("cashbackId") Long cashbackId, @Param("requestId") Long requestId);

  @Update("UPDATE cashback_records SET withdrawal_request_id = NULL WHERE withdrawal_request_id = #{requestId} AND status = 'PENDING'")
  int clearWithdrawalRequest(@Param("requestId") Long requestId);

  @Update("UPDATE cashback_records SET withdrawal_request_id = NULL WHERE id = #{cashbackId} AND status = 'PENDING'")
  int clearWithdrawalRequestByCashbackId(@Param("cashbackId") Long cashbackId);

  @Update("UPDATE cashback_records SET amount = #{amount}, remark = #{remark} WHERE id = #{id}")
  int updateAmountAndRemark(
      @Param("id") Long id,
      @Param("amount") java.math.BigDecimal amount,
      @Param("remark") String remark
  );

  @Update("UPDATE cashback_records SET amount = #{amount}, status = #{status}, remark = #{remark} WHERE id = #{id}")
  int updateAmountStatusAndRemark(
      @Param("id") Long id,
      @Param("amount") java.math.BigDecimal amount,
      @Param("status") String status,
      @Param("remark") String remark
  );

  @Select("SELECT COALESCE(SUM(amount),0) FROM cashback_records WHERE status <> 'CANCELLED'")
  Double totalAmount();

  @Select("SELECT COALESCE(SUM(amount),0) FROM cashback_records WHERE status <> 'CANCELLED' AND DATE(created_at) = CURRENT_DATE")
  Double todayAmount();
}
