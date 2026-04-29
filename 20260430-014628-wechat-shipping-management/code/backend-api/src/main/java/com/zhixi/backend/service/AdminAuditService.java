package com.zhixi.backend.service;

import com.zhixi.backend.dto.AdminPageResult;
import com.zhixi.backend.mapper.AdminOperationLogMapper;
import com.zhixi.backend.model.AdminOperationLog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuditService {
  private final AdminOperationLogMapper adminOperationLogMapper;

  public AdminAuditService(AdminOperationLogMapper adminOperationLogMapper) {
    this.adminOperationLogMapper = adminOperationLogMapper;
  }

  public void log(AdminOperationLog log) {
    adminOperationLogMapper.insert(log);
  }

  public AdminPageResult<AdminOperationLog> page(Integer page, Integer size) {
    int safePage = (page == null || page < 1) ? 1 : page;
    int safeSize = (size == null || size < 1) ? 20 : Math.min(size, 100);
    int offset = (safePage - 1) * safeSize;
    long total = adminOperationLogMapper.countAll();
    List<AdminOperationLog> records = adminOperationLogMapper.findPage(offset, safeSize);
    return new AdminPageResult<>(records, total, safePage, safeSize);
  }
}
