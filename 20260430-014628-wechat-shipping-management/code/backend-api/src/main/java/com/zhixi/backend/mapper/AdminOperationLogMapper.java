package com.zhixi.backend.mapper;

import com.zhixi.backend.model.AdminOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminOperationLogMapper {
  @Insert("INSERT INTO admin_operation_logs(admin_id, module, action, target_type, target_id, request_payload, created_at) " +
      "VALUES(#{adminId}, #{module}, #{action}, #{targetType}, #{targetId}, #{requestPayload}, NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(AdminOperationLog log);

  @Select("SELECT COUNT(1) FROM admin_operation_logs")
  long countAll();

  @Select("SELECT l.id,l.admin_id AS adminId,a.display_name AS adminName,l.module,l.action,l.target_type AS targetType,l.target_id AS targetId,l.request_payload AS requestPayload,l.created_at AS createdAt " +
      "FROM admin_operation_logs l LEFT JOIN admins a ON a.id = l.admin_id ORDER BY l.id DESC LIMIT #{limit} OFFSET #{offset}")
  List<AdminOperationLog> findPage(@Param("offset") int offset, @Param("limit") int limit);
}
