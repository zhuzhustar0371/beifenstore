package com.zhixi.backend.config;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.model.AdminOperationLog;
import com.zhixi.backend.model.Admin;
import com.zhixi.backend.service.AdminAuditService;
import com.zhixi.backend.service.AdminAuthService;
import com.zhixi.backend.service.AdminPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
  private final AdminAuthService adminAuthService;
  private final AdminPermissionService adminPermissionService;
  private final AdminAuditService adminAuditService;
  private final ObjectMapper objectMapper;

  public AdminAuthInterceptor(
      AdminAuthService adminAuthService,
      AdminPermissionService adminPermissionService,
      AdminAuditService adminAuditService,
      ObjectMapper objectMapper
  ) {
    this.adminAuthService = adminAuthService;
    this.adminPermissionService = adminPermissionService;
    this.adminAuditService = adminAuditService;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // 允许 CORS 的 preflight 请求直接放行
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String path = request.getRequestURI();
    if (path.startsWith("/api/admin/auth")) {
      return true;
    }

    try {
      String token = extractToken(request);
      Admin admin = adminAuthService.getAdminByToken(token);
      Set<String> permissions = adminPermissionService.getPermissions(admin);
      if (!adminPermissionService.hasPermission(permissions, request.getMethod(), path)) {
        writeForbidden(response, "当前账号无权限访问该功能");
        return false;
      }
      request.setAttribute("adminId", admin.getId());
      request.setAttribute("adminName", admin.getDisplayName());
      request.setAttribute("adminPermissions", permissions);
      return true;
    } catch (BusinessException ex) {
      writeUnauthorized(response, ex.getMessage());
      return false;
    }
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    String method = request.getMethod();
    if ("GET".equals(method) || "OPTIONS".equals(method)) {
      return;
    }
    Object adminIdObj = request.getAttribute("adminId");
    if (!(adminIdObj instanceof Long adminId)) {
      return;
    }
    String adminName = String.valueOf(request.getAttribute("adminName"));
    AdminOperationLog log = new AdminOperationLog();
    log.setAdminId(adminId);
    log.setAdminName(adminName);
    log.setModule(request.getRequestURI());
    log.setAction(method);
    log.setTargetType("API");
    log.setTargetId(null);
    log.setRequestPayload("status=" + response.getStatus() + ",ip=" + resolveClientIp(request));
    try {
      adminAuditService.log(log);
    } catch (Exception ignored) {
      // 审计日志异常不影响主流程
    }
  }

  private String extractToken(HttpServletRequest request) {
    String auth = request.getHeader("Authorization");
    if (auth == null || auth.isBlank()) {
      throw new BusinessException("请先登录后台");
    }
    String prefix = "Bearer ";
    if (!auth.startsWith(prefix)) {
      throw new BusinessException("无效的授权头");
    }
    return auth.substring(prefix.length()).trim();
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(message)));
  }

  private void writeForbidden(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(message)));
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
