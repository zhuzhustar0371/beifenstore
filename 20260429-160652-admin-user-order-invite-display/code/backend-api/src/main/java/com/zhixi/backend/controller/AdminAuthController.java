package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.dto.AdminLoginRequest;
import com.zhixi.backend.dto.AdminLoginResponse;
import com.zhixi.backend.model.Admin;
import com.zhixi.backend.service.AdminAuthService;
import com.zhixi.backend.service.AdminPermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
  private final AdminAuthService adminAuthService;
  private final AdminPermissionService adminPermissionService;

  public AdminAuthController(AdminAuthService adminAuthService, AdminPermissionService adminPermissionService) {
    this.adminAuthService = adminAuthService;
    this.adminPermissionService = adminPermissionService;
  }

  @PostMapping("/login")
  public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
    return ApiResponse.ok(adminAuthService.login(request));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
    adminAuthService.logout(extractToken(authorization));
    return ApiResponse.ok(null);
  }

  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
    Admin admin = adminAuthService.getAdminByToken(extractToken(authorization));
    Map<String, Object> data = new HashMap<>();
    data.put("id", admin.getId());
    data.put("username", admin.getUsername());
    data.put("displayName", admin.getDisplayName());
    data.put("roleCode", admin.getRoleCode() == null ? "SUPER_ADMIN" : admin.getRoleCode());
    data.put("permissions", adminPermissionService.getPermissions(admin));
    return ApiResponse.ok(data);
  }

  private String extractToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return "";
    }
    String prefix = "Bearer ";
    return authorization.startsWith(prefix) ? authorization.substring(prefix.length()).trim() : authorization.trim();
  }
}
