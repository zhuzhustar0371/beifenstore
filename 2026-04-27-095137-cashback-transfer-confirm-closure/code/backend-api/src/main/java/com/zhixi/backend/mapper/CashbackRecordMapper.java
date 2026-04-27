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
  @Insert("INSERT INTO cashback_records(user_id,related_order_id,cashback_type,amount,related_invite_batch_id,status,remark,out_batch_no,out_detail_no,created_at) " +
      "VALUES(#{userId},#{orderId},#{type},#{amount},#{batchNo},#{status},#{remark},#{outBatchNo},#{outDetailNo},NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(CashbackRecord record);

  @Select("SELECT id,user_id AS userId,related_order_id AS orderId,cashback_type AS type,amount,related_invite_batch_id AS batchNo,status,remark,out_batch_no AS outBatchNo,out_detail_no AS outDetailNo,transfer_id AS transferId,transfer_detail_id AS transferDetailId,transfer_fail_reason AS transferFailReason,transfer_package_info AS transferPackageInfo,transfer_time AS transferTime,created_at AS createdAt " +
      "FROM cashback_records WHERE id = #{id}")
  CashbackRecord findById(Long id);

  @Select("SELECT id,user_id AS userId,related_order_id AS orderId,cashback_type AS type,amount,related_invite_batch_id AS batchNo,status,remark,out_batch_no AS outBatchNo,out_detail_no AS outDetailNo,transfer_id AS transferId,transfer_detail_id AS transferDetailId,transfer_fail_reason AS transferFailReason,transfer_package_info AS transferPackageInfo,transfer_time AS transferTime,created_at AS createdAt " +
      "FROM cashback_records WHERE user_id = #{userId} ORDER BY id DESC")
  List<CashbackRecord> findByUserId(Long userId);

  @Select("SELECT id,user_id AS userId,related_order_id AS orderId,cashback_type AS type,amount,related_invite_batch_id AS batchNo,status,remark,out_batch_no AS outBatchNo,out_detail_no AS outDetailNo,transfer_id AS transferId,transfer_detail_id AS transferDetailId,transfer_fail_reason AS transferFailReason,transfer_package_info AS transferPackageInfo,transfer_time AS transferTime,created_at AS createdAt " +
      "FROM cashback_records WHERE related_order_id = #{orderId} ORDER BY id DESC")
  List<CashbackRecord> findByOrderId(Long orderId);

  @Select("SELECT id,user_id AS userId,related_order_id AS orderId,cashback_type AS type,amount,related_invite_batch_id AS batchNo,status,remark,out_batch_no AS outBatchNo,out_detail_no AS outDetailNo,transfer_id AS transferId,transfer_detail_id AS transferDetailId,transfer_fail_reason AS transferFailReason,transfer_package_info AS transferPackageInfo,transfer_time AS transferTime,created_at AS createdAt " +
      "FROM cashback_records ORDER BY id DESC")
  List<CashbackRecord> findAll();

  @Select("SELECT id,user_id AS userId,related_order_id AS orderId,cashback_type AS type,amount,related_invite_batch_id AS batchNo,status,remark,out_batch_no AS outBatchNo,out_detail_no AS outDetailNo,transfer_id AS transferId,transfer_detail_id AS transferDetailId,transfer_fail_reason AS transferFailReason,transfer_package_info AS transferPackageInfo,transfer_time AS transferTime,created_at AS createdAt " +
      "FROM cashback_records WHERE user_id = #{userId} AND cashback_type = 'INVITE_BATCH' ORDER BY related_invite_batch_id DESC, id DESC")
  List<CashbackRecord> findInviteBatchByUserId(Long userId);

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM cashback_records",
      "WHERE 1=1",
      "<if test='userId != null'>",
      "  AND user_id = #{userId}",
      "</if>",
      "<if test='type != null and type != \"\"'>",
      "  AND cashback_type = #{type}",
      "</if>",
      "<if test='status != null and status != \"\"'>",
      "  AND status = #{status}",
      "</if>",
      "</script>"
  })
  long countByAdminQuery(
      @Param("userId") Long userId,
      @Param("type") String type,
      @Param("status") String status
  );

  @Select({
      "<script>",
      "SELECT id,user_id AS userId,related_order_id AS orderId,cashback_type AS type,amount,related_invite_batch_id AS batchNo,status,remark,out_batch_no AS outBatchNo,out_detail_no AS outDetailNo,transfer_id AS transferId,transfer_detail_id AS transferDetailId,transfer_fail_reason AS transferFailReason,transfer_package_info AS transferPackageInfo,transfer_time AS transferTime,created_at AS createdAt FROM cashback_records",
      "WHERE 1=1",
      "<if test='userId != null'>",
      "  AND user_id = #{userId}",
      "</if>",
      "<if test='type != null and type != \"\"'>",
      "  AND cashback_type = #{type}",
      "</if>",
      "<if test='status != null and status != \"\"'>",
      "  AND status = #{status}",
      "</if>",
      "ORDER BY id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<CashbackRecord> findByAdminQuery(
      @Param("userId") Long userId,
      @Param("type") String type,
      @Param("status") String status,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Select("SELECT COUNT(1) FROM cashback_records WHERE user_id = #{userId} AND cashback_type = 'INVITE_BATCH' AND related_invite_batch_id = #{batchNo}")
  int existsInviteBatch(@Param("userId") Long userId, @Param("batchNo") Integer batchNo);

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

  @Update("UPDATE cashback_records SET status = 'CANCELLED', remark = CONCAT(COALESCE(remark, ''), ' | 全局重置统计') WHERE status = 'PENDING'")
  int cancelAllPending();

  @Select("SELECT COALESCE(SUM(amount),0) FROM cashback_records WHERE status <> 'CANCELLED'")
  Double totalAmount();

  @Select("SELECT COALESCE(SUM(amount),0) FROM cashback_records WHERE status <> 'CANCELLED' AND DATE(created_at) = CURRENT_DATE")
  Double todayAmount();
}

