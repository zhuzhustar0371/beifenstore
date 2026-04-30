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
      throw new BusinessException("账号不存在或已禁用");
    }

    String passwordHash = PasswordCodec.sha256(request.getPassword());
    if (!passwordHash.equals(admin.getPasswordHash())) {
      throw new BusinessException("账号或密码错误");
    }

    adminSessionMapper.deleteExpired();
    String token = UUID.randomUUID().toString().replace("-", "");
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
      throw new BusinessException("登录状态无效");
    }
    adminSessionMapper.deleteExpired();
    AdminSession session = adminSessionMapper.findByToken(token);
    if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("登录状态已过期");
    }
    Admin admin = adminMapper.findById(session.getAdminId());
    if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
      throw new BusinessException("管理员不可用");
    }
    return admin;
  }

  public Admin verifyCurrentAdminPassword(String token, String rawPassword) {
    Admin admin = getAdminByToken(token);
    if (rawPassword == null || rawPassword.isBlank()) {
      throw new BusinessException("管理员密码不能为空");
    }
    String passwordHash = PasswordCodec.sha256(rawPassword);
    if (!passwordHash.equals(admin.getPasswordHash())) {
      throw new BusinessException("管理员密码错误");
    }
    return admin;
  }
}
