package com.zhixi.backend.mapper;

import com.zhixi.backend.model.WithdrawalRequest;
import com.zhixi.backend.model.WithdrawalRequestItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WithdrawalRequestMapper {
  @Insert("INSERT INTO withdrawal_requests(user_id,amount,status,source,remark,created_at) " +
      "VALUES(#{userId},#{amount},#{status},#{source},#{remark},NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(WithdrawalRequest request);

  @Insert("INSERT INTO withdrawal_request_items(request_id,cashback_record_id,amount,created_at) " +
      "VALUES(#{requestId},#{cashbackId},#{amount},NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertItem(WithdrawalRequestItem item);

  @Select("SELECT id,user_id AS userId,amount,status,source,remark,created_at AS createdAt,approved_at AS approvedAt,completed_at AS completedAt " +
      "FROM withdrawal_requests WHERE id = #{id}")
  WithdrawalRequest findById(Long id);

  @Select("SELECT id,user_id AS userId,amount,status,source,remark,created_at AS createdAt,approved_at AS approvedAt,completed_at AS completedAt " +
      "FROM withdrawal_requests WHERE user_id = #{userId} ORDER BY id DESC")
  List<WithdrawalRequest> findByUserId(Long userId);

  @Select({
      "<script>",
      "SELECT id,user_id AS userId,amount,status,source,remark,created_at AS createdAt,approved_at AS approvedAt,completed_at AS completedAt",
      "FROM withdrawal_requests",
      "WHERE 1=1",
      "<if test='status != null and status != \"\"'>",
      "  AND status = #{status}",
      "</if>",
      "ORDER BY id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<WithdrawalRequest> findByAdminQuery(
      @Param("status") String status,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM withdrawal_requests",
      "WHERE 1=1",
      "<if test='status != null and status != \"\"'>",
      "  AND status = #{status}",
      "</if>",
      "</script>"
  })
  long countByAdminQuery(@Param("status") String status);

  @Select("SELECT id,request_id AS requestId,cashback_record_id AS cashbackId,amount,created_at AS createdAt " +
      "FROM withdrawal_request_items WHERE request_id = #{requestId} ORDER BY id ASC")
  List<WithdrawalRequestItem> findItemsByRequestId(Long requestId);

  @Update("UPDATE withdrawal_requests SET status = #{status}, remark = #{remark}, approved_at = CASE WHEN #{status} = 'APPROVED' THEN NOW() ELSE approved_at END, completed_at = CASE WHEN #{status} = 'COMPLETED' THEN NOW() ELSE completed_at END WHERE id = #{id}")
  int updateStatus(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("remark") String remark
  );

  @Update("UPDATE withdrawal_requests SET amount = #{amount} WHERE id = #{id}")
  int updateAmount(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
}
