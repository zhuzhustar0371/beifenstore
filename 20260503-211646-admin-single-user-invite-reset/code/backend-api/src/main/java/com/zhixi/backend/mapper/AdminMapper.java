package com.zhixi.backend.mapper;

import com.zhixi.backend.model.Admin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AdminMapper {
  @Insert("INSERT INTO admins(username, password_hash, display_name, status, created_at, updated_at) " +
      "VALUES(#{username}, #{passwordHash}, #{displayName}, #{status}, NOW(), NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Admin admin);
  @Select("SELECT a.id,a.username,a.password_hash AS passwordHash,a.display_name AS displayName,a.mobile,a.status,a.last_login_at AS lastLoginAt,a.created_at AS createdAt,a.updated_at AS updatedAt," +
      "(SELECT ar.role_code FROM admin_role_rel rr JOIN admin_roles ar ON rr.role_id = ar.id WHERE rr.admin_id = a.id ORDER BY rr.id ASC LIMIT 1) AS roleCode " +
      "FROM admins a WHERE a.username = #{username}")
  Admin findByUsername(String username);

  @Select("SELECT a.id,a.username,a.password_hash AS passwordHash,a.display_name AS displayName,a.mobile,a.status,a.last_login_at AS lastLoginAt,a.created_at AS createdAt,a.updated_at AS updatedAt," +
      "(SELECT ar.role_code FROM admin_role_rel rr JOIN admin_roles ar ON rr.role_id = ar.id WHERE rr.admin_id = a.id ORDER BY rr.id ASC LIMIT 1) AS roleCode " +
      "FROM admins a WHERE a.id = #{id}")
  Admin findById(Long id);

  @Update("UPDATE admins SET last_login_at = #{time}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  int updateLastLogin(@Param("id") Long id, @Param("time") LocalDateTime time);
}
