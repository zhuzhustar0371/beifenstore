package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.common.PasswordCodec;
import com.zhixi.backend.dto.AdminLoginRequest;
import com.zhixi.backend.dto.AdminLoginResponse;
import com.zhixi.backend.mapper.AdminMapper;
import com.zhixi.backend.mapper.AdminSessionMapper;
import com.zhixi.backend.model.Admin;
import com.zhixi.backend.model.AdminSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AdminAuthService {
  private static final int SESSION_HOURS = 12;

  private final AdminMapper adminMapper;
  private final AdminSessionMapper adminSessionMapper;

  public AdminAuthService(AdminMapper adminMapper, AdminSessionMapper adminSessionMapper) {
    this.adminMapper = adminMapper;
    this.adminSessionMapper = adminSessionMapper;
  }

  @Transactional
  public AdminLoginResponse login(AdminLoginRequest request) {
    Admin admin = adminMapper.findByUsername(request.getUsername());
    if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
      throw new BusinessException("璐﹀彿涓嶅瓨鍦ㄦ垨宸茬鐢?);
    }

    String passwordHash =REMOTE_BACKUP_REDACTED
    if (!passwordHash.equals(admin.getPasswordHash())) {
      throw new BusinessException("璐﹀彿鎴栧瘑鐮侀敊璇?);
    }

    adminSessionMapper.deleteExpired();
    String token =REMOTE_BACKUP_REDACTED
    AdminSession session = new AdminSession();
    session.setAdminId(admin.getId());
    session.setToken(token);
    session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_HOURS));
    adminSessionMapper.insert(session);
    adminMapper.updateLastLogin(admin.getId(), LocalDateTime.now());

    AdminLoginResponse response = new AdminLoginResponse();
    response.setAdminId(admin.getId());
    response.setUsername(admin.getUsername());
    response.setDisplayName(admin.getDisplayName());
    response.setToken(token);
    return response;
  }

  @Transactional
  public void logout(String token) {
    if (token == null || token.isBlank()) {
      return;
    }
    adminSessionMapper.deleteByToken(token);
  }

  public Admin getAdminByToken(String token) {
    if (token == null || token.isBlank()) {
      throw new BusinessException("鐧诲綍鐘舵€佹棤鏁?);
    }
    adminSessionMapper.deleteExpired();
    AdminSession session = adminSessionMapper.findByToken(token);
    if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("鐧诲綍鐘舵€佸凡杩囨湡");
    }
    Admin admin = adminMapper.findById(session.getAdminId());
    if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
      throw new BusinessException("绠＄悊鍛樹笉鍙敤");
    }
    return admin;
  }
}

