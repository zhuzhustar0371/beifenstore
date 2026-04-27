package com.zhixi.backend.service;

import com.zhixi.backend.model.Admin;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AdminPermissionService {
  private static final Set<String> ALL_PERMISSIONS = Set.of(
      "dashboard:read",
      "users:read", "users:write",
      "orders:read", "orders:write",
      "products:read", "products:write",
      "invites:read",
      "cashbacks:read", "cashbacks:write",
      "audit:read"
  );

  private static final Map<String, Set<String>> ROLE_PERMISSIONS;

  static {
    Map<String, Set<String>> map = new HashMap<>();
    map.put("SUPER_ADMIN", ALL_PERMISSIONS);
    map.put("OPERATOR", Set.of(
        "dashboard:read",
        "users:read",
        "orders:read",
        "products:read",
        "invites:read",
        "cashbacks:read"
    ));
    ROLE_PERMISSIONS = Collections.unmodifiableMap(map);
  }

  public Set<String> getPermissions(Admin admin) {
    String roleCode = admin.getRoleCode() == null || admin.getRoleCode().isBlank() ? "SUPER_ADMIN" : admin.getRoleCode();
    return new HashSet<>(ROLE_PERMISSIONS.getOrDefault(roleCode, Set.of()));
  }

  public boolean hasPermission(Set<String> permissions, String method, String path) {
    String required = requiredPermission(method, path);
    if (required == null) {
      return true;
    }
    return permissions.contains(required);
  }

  private String requiredPermission(String method, String path) {
    if (path.endsWith("/dashboard")) return "dashboard:read";
    if (path.startsWith("/api/admin/users")) {
      return "GET".equals(method) ? "users:read" : "users:write";
    }
    if (path.startsWith("/api/admin/orders")) {
      return "GET".equals(method) ? "orders:read" : "orders:write";
    }
    if (path.startsWith("/api/admin/products")) {
      return "GET".equals(method) ? "products:read" : "products:write";
    }
    if (path.startsWith("/api/admin/invites")) {
      return "invites:read";
    }
    if (path.startsWith("/api/admin/cashbacks")) {
      return "GET".equals(method) ? "cashbacks:read" : "cashbacks:write";
    }
    if (Pattern.matches("^/api/admin/audit-logs.*$", path)) {
      return "audit:read";
    }
    return null;
  }
}
