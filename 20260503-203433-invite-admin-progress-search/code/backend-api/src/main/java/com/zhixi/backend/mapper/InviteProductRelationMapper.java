package com.zhixi.backend.mapper;

import com.zhixi.backend.model.InviteProductRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InviteProductRelationMapper {
  String INVITE_PRODUCT_COLUMNS =
      "id,inviter_user_id AS inviterId,invitee_user_id AS inviteeId,product_id AS productId,"
          + "bind_time AS boundAt,first_paid_time AS firstPaidAt ";

  @Insert("INSERT IGNORE INTO invite_product_relations(inviter_user_id,invitee_user_id,product_id,bind_time) "
      + "VALUES(#{inviterId},#{inviteeId},#{productId},CURRENT_TIMESTAMP)")
  int insertIgnore(
      @Param("inviterId") Long inviterId,
      @Param("inviteeId") Long inviteeId,
      @Param("productId") Long productId
  );

  @Select("SELECT " + INVITE_PRODUCT_COLUMNS + "FROM invite_product_relations "
      + "WHERE invitee_user_id = #{inviteeId} AND product_id = #{productId} LIMIT 1")
  InviteProductRelation findByInviteeIdAndProductId(@Param("inviteeId") Long inviteeId, @Param("productId") Long productId);

  @Update("UPDATE invite_product_relations SET first_paid_time = #{time} "
      + "WHERE invitee_user_id = #{inviteeId} AND product_id = #{productId} AND first_paid_time IS NULL")
  int markFirstPaid(
      @Param("inviteeId") Long inviteeId,
      @Param("productId") Long productId,
      @Param("time") LocalDateTime time
  );

  @Update("UPDATE invite_product_relations SET first_paid_time = #{time} "
      + "WHERE invitee_user_id = #{inviteeId} AND product_id = #{productId}")
  int updateFirstPaid(
      @Param("inviteeId") Long inviteeId,
      @Param("productId") Long productId,
      @Param("time") LocalDateTime time
  );

  @Update("UPDATE invite_product_relations SET first_paid_time = NULL "
      + "WHERE invitee_user_id = #{inviteeId} AND product_id = #{productId} AND first_paid_time = #{time}")
  int clearFirstPaid(
      @Param("inviteeId") Long inviteeId,
      @Param("productId") Long productId,
      @Param("time") LocalDateTime time
  );

  @Update("UPDATE invite_product_relations SET first_paid_time = NULL WHERE first_paid_time IS NOT NULL")
  int clearAllFirstPaid();

  @Delete("DELETE FROM invite_product_relations")
  int deleteAll();

  @Select("SELECT COUNT(1) FROM invite_product_relations "
      + "WHERE inviter_user_id = #{inviterId} AND product_id = #{productId} AND first_paid_time IS NOT NULL")
  int countInviteeFirstPaid(@Param("inviterId") Long inviterId, @Param("productId") Long productId);

  @Select("SELECT " + INVITE_PRODUCT_COLUMNS + "FROM invite_product_relations "
      + "WHERE inviter_user_id = #{inviterId} AND product_id = #{productId} AND first_paid_time IS NOT NULL "
      + "ORDER BY first_paid_time ASC, id ASC")
  List<InviteProductRelation> findFirstPaidByInviterAndProduct(
      @Param("inviterId") Long inviterId,
      @Param("productId") Long productId
  );

  @Select("SELECT " + INVITE_PRODUCT_COLUMNS + "FROM invite_product_relations "
      + "WHERE inviter_user_id = #{inviterId} AND invitee_user_id = #{inviteeId} "
      + "ORDER BY product_id ASC")
  List<InviteProductRelation> findByInviterAndInvitee(
      @Param("inviterId") Long inviterId,
      @Param("inviteeId") Long inviteeId
  );
}
