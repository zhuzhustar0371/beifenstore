package com.zhixi.backend.mapper;

import com.zhixi.backend.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {
  @Insert("INSERT INTO users(mobile,password_hash,nickname,invite_code,inviter_user_id,status,miniapp_openid) VALUES(#{phone},#{passwordHash},#{nickname},#{inviteCode},#{inviterId},1,#{miniappOpenid})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(User user);

  @Select("SELECT id,mobile AS phone,password_hash AS passwordHash,nickname,invite_code AS inviteCode,inviter_user_id AS inviterId,status,miniapp_openid AS miniappOpenid,cashback_reset_at AS cashbackResetAt,created_at AS createdAt FROM users WHERE id = #{id}")
  User findById(Long id);

  @Select("SELECT id,mobile AS phone,password_hash AS passwordHash,nickname,invite_code AS inviteCode,inviter_user_id AS inviterId,status,miniapp_openid AS miniappOpenid,cashback_reset_at AS cashbackResetAt,created_at AS createdAt FROM users WHERE mobile = #{phone}")
  User findByPhone(String phone);

  @Select("SELECT id,mobile AS phone,password_hash AS passwordHash,nickname,invite_code AS inviteCode,inviter_user_id AS inviterId,status,miniapp_openid AS miniappOpenid,cashback_reset_at AS cashbackResetAt,created_at AS createdAt FROM users WHERE invite_code = #{inviteCode}")
  User findByInviteCode(String inviteCode);

  @Select("SELECT COUNT(1) FROM users")
  long countAll();

  @Select("SELECT id,mobile AS phone,password_hash AS passwordHash,nickname,invite_code AS inviteCode,inviter_user_id AS inviterId,status,miniapp_openid AS miniappOpenid,cashback_reset_at AS cashbackResetAt,created_at AS createdAt FROM users ORDER BY id DESC")
  List<User> findAll();

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM users u",
      "WHERE 1=1",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND (u.mobile LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.nickname LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.invite_code LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.miniapp_openid LIKE CONCAT('%', #{keyword}, '%')",
      "       OR EXISTS (",
      "         SELECT 1 FROM user_wechat_auth uwa",
      "         WHERE uwa.user_id = u.id",
      "           AND (uwa.nickname LIKE CONCAT('%', #{keyword}, '%') OR uwa.openid LIKE CONCAT('%', #{keyword}, '%'))",
      "       ))",
      "</if>",
      "<if test='status != null'>",
      "  AND u.status = #{status}",
      "</if>",
      "</script>"
  })
  long countByAdminQuery(@Param("keyword") String keyword, @Param("status") Integer status);

  @Select({
      "<script>",
      "SELECT u.id,u.mobile AS phone,u.nickname,u.invite_code AS inviteCode,u.inviter_user_id AS inviterId,u.status,u.miniapp_openid AS miniappOpenid,u.cashback_reset_at AS cashbackResetAt,u.created_at AS createdAt",
      "FROM users u",
      "WHERE 1=1",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND (u.mobile LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.nickname LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.invite_code LIKE CONCAT('%', #{keyword}, '%')",
      "       OR u.miniapp_openid LIKE CONCAT('%', #{keyword}, '%')",
      "       OR EXISTS (",
      "         SELECT 1 FROM user_wechat_auth uwa",
      "         WHERE uwa.user_id = u.id",
      "           AND (uwa.nickname LIKE CONCAT('%', #{keyword}, '%') OR uwa.openid LIKE CONCAT('%', #{keyword}, '%'))",
      "       ))",
      "</if>",
      "<if test='status != null'>",
      "  AND u.status = #{status}",
      "</if>",
      "ORDER BY u.id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<User> findByAdminQuery(
      @Param("keyword") String keyword,
      @Param("status") Integer status,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Update("UPDATE users SET status = #{status} WHERE id = #{id}")
  int updateStatus(@Param("id") Long id, @Param("status") Integer status);

  @Select("SELECT COUNT(1) FROM users WHERE DATE(created_at) = CURRENT_DATE")
  long countToday();

  @Update("UPDATE users SET inviter_user_id = #{inviterId} WHERE id = #{id} AND inviter_user_id IS NULL")
  int bindInviter(@Param("id") Long id, @Param("inviterId") Long inviterId);

  @Update("UPDATE users SET inviter_user_id = NULL WHERE inviter_user_id IS NOT NULL")
  int clearAllInviterBindings();

  @Update("UPDATE users SET miniapp_openid = #{openid} WHERE id = #{id}")
  int updateMiniappOpenid(@Param("id") Long id, @Param("openid") String openid);

  @Update("UPDATE users SET nickname = #{nickname} WHERE id = #{id}")
  int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

  @Select("SELECT id,mobile AS phone,password_hash AS passwordHash,nickname,invite_code AS inviteCode,inviter_user_id AS inviterId,status,miniapp_openid AS miniappOpenid,cashback_reset_at AS cashbackResetAt,created_at AS createdAt FROM users WHERE miniapp_openid = #{openid}")
  User findByMiniappOpenid(String openid);

  @Update("UPDATE users SET password_hash = #{passwordHash} WHERE mobile = #{phone}")
  int updatePasswordByPhone(@Param("phone") String phone, @Param("passwordHash") String passwordHash);

  @Update("UPDATE users SET cashback_reset_at = NOW()")
  int resetAllCashbackStats();
}
