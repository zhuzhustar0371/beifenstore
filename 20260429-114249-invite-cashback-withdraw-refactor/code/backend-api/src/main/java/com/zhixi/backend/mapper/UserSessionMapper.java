package com.zhixi.backend.mapper;

import com.zhixi.backend.model.UserSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserSessionMapper {
  @Insert("INSERT INTO user_sessions(user_id, token, login_type, expires_at) VALUES(#{userId}, #{token}, #{loginType}, #{expiresAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(UserSession session);

  @Select("SELECT * FROM user_sessions WHERE token = #{token}")
  UserSession findByToken(String token);

  @Delete("DELETE FROM user_sessions WHERE token = #{token}")
  int deleteByToken(String token);

  @Delete("DELETE FROM user_sessions WHERE expires_at < CURRENT_TIMESTAMP")
  int deleteExpired();
}
