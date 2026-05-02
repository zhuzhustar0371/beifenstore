package com.zhixi.backend.mapper;

import com.zhixi.backend.model.AdminSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminSessionMapper {
  @Insert("INSERT INTO admin_sessions(admin_id, token, expires_at) VALUES(#{adminId}, #{token}, #{expiresAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(AdminSession session);

  @Select("SELECT * FROM admin_sessions WHERE token = #{token}")
  AdminSession findByToken(String token);

  @Delete("DELETE FROM admin_sessions WHERE token = #{token}")
  int deleteByToken(String token);

  @Delete("DELETE FROM admin_sessions WHERE expires_at < CURRENT_TIMESTAMP")
  int deleteExpired();
}
