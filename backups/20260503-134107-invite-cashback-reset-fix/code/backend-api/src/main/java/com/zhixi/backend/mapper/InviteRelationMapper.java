package com.zhixi.backend.mapper;

import com.zhixi.backend.model.InviteRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InviteRelationMapper {
  @Insert("INSERT INTO invite_relations(inviter_user_id,invitee_user_id,bind_time) VALUES(#{inviterId},#{inviteeId},CURRENT_TIMESTAMP)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(InviteRelation relation);

  @Select("SELECT id,inviter_user_id AS inviterId,invitee_user_id AS inviteeId,bind_time AS boundAt,first_paid_time AS firstPaidAt FROM invite_relations WHERE invitee_user_id = #{inviteeId}")
  InviteRelation findByInviteeId(Long inviteeId);

  @Select("SELECT id,inviter_user_id AS inviterId,invitee_user_id AS inviteeId,bind_time AS boundAt,first_paid_time AS firstPaidAt FROM invite_relations WHERE inviter_user_id = #{inviterId} ORDER BY id DESC")
  List<InviteRelation> findByInviterId(Long inviterId);

  @Update("UPDATE invite_relations SET first_paid_time = #{time} WHERE invitee_user_id = #{inviteeId} AND first_paid_time IS NULL")
  int markFirstPaid(@Param("inviteeId") Long inviteeId, @Param("time") LocalDateTime time);

  @Update("UPDATE invite_relations SET first_paid_time = NULL WHERE invitee_user_id = #{inviteeId} AND first_paid_time = #{time}")
  int clearFirstPaid(@Param("inviteeId") Long inviteeId, @Param("time") LocalDateTime time);

  @Update("UPDATE invite_relations SET first_paid_time = NULL WHERE first_paid_time IS NOT NULL")
  int clearAllFirstPaid();

  @Delete("DELETE FROM invite_relations")
  int deleteAll();

  @Select("SELECT COUNT(1) FROM invite_relations WHERE inviter_user_id = #{inviterId} AND first_paid_time IS NOT NULL")
  int countInviteeFirstPaid(Long inviterId);

  @Select("SELECT id,inviter_user_id AS inviterId,invitee_user_id AS inviteeId,bind_time AS boundAt,first_paid_time AS firstPaidAt FROM invite_relations ORDER BY id DESC")
  List<InviteRelation> findAll();

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM invite_relations",
      "WHERE 1=1",
      "<if test='inviterId != null'>",
      "  AND inviter_user_id = #{inviterId}",
      "</if>",
      "<if test='inviteeId != null'>",
      "  AND invitee_user_id = #{inviteeId}",
      "</if>",
      "<if test='firstPaidOnly != null and firstPaidOnly'>",
      "  AND first_paid_time IS NOT NULL",
      "</if>",
      "</script>"
  })
  long countByAdminQuery(
      @Param("inviterId") Long inviterId,
      @Param("inviteeId") Long inviteeId,
      @Param("firstPaidOnly") Boolean firstPaidOnly
  );

  @Select({
      "<script>",
      "SELECT id,inviter_user_id AS inviterId,invitee_user_id AS inviteeId,bind_time AS boundAt,first_paid_time AS firstPaidAt",
      "FROM invite_relations",
      "WHERE 1=1",
      "<if test='inviterId != null'>",
      "  AND inviter_user_id = #{inviterId}",
      "</if>",
      "<if test='inviteeId != null'>",
      "  AND invitee_user_id = #{inviteeId}",
      "</if>",
      "<if test='firstPaidOnly != null and firstPaidOnly'>",
      "  AND first_paid_time IS NOT NULL",
      "</if>",
      "ORDER BY id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<InviteRelation> findByAdminQuery(
      @Param("inviterId") Long inviterId,
      @Param("inviteeId") Long inviteeId,
      @Param("firstPaidOnly") Boolean firstPaidOnly,
      @Param("offset") int offset,
      @Param("limit") int limit
  );
}
