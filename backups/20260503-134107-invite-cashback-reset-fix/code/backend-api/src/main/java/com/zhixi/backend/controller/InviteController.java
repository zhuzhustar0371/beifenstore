package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.MiniappInviteRecordVO;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.User;
import com.zhixi.backend.service.InviteService;
import com.zhixi.backend.service.UserAuthService;
import com.zhixi.backend.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invites")
public class InviteController {
  private final InviteService inviteService;
  private final UserAuthService userAuthService;
  private final UserService userService;

  public InviteController(InviteService inviteService, UserAuthService userAuthService, UserService userService) {
    this.inviteService = inviteService;
    this.userAuthService = userAuthService;
    this.userService = userService;
  }

  @GetMapping("/{userId}")
  public ApiResponse<List<InviteRelation>> list(@PathVariable Long userId) {
    return ApiResponse.ok(inviteService.listByInviter(userId));
  }

  @GetMapping("/me/status")
  public ApiResponse<Map<String, Object>> myInviteStatus(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User user = userAuthService.getUserByToken(extractToken(authorization));
    return ApiResponse.ok(Map.of("hasInviter", user.getInviterId() != null));
  }

  @PostMapping("/me/bind-by-code")
  public ApiResponse<Void> bindInviterByCode(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody Map<String, String> body
  ) {
    User user = userAuthService.getUserByToken(extractToken(authorization));
    if (user.getInviterId() != null) {
      throw new BusinessException("你已绑定邀请关系");
    }
    String inviteCode = body == null ? null : body.get("inviteCode");
    if (inviteCode == null || inviteCode.isBlank()) {
      throw new BusinessException("请输入邀请码");
    }
    userService.bindInviterIfNeeded(user.getId(), inviteCode.trim());
    return ApiResponse.ok(null);
  }

  @GetMapping("/me/records")
  public ApiResponse<List<MiniappInviteRecordVO>> listMyRecords(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User user = userAuthService.getUserByToken(extractToken(authorization));
    return ApiResponse.ok(inviteService.listMiniappRecordsByInviter(user.getId()));
  }

  @GetMapping("/me/qrcode")
  public ResponseEntity<byte[]> qrcode(@RequestHeader(value = "Authorization", required = false) String authorization) {
    User user = userAuthService.getUserByToken(extractToken(authorization));
    byte[] image = inviteService.createMiniappInviteCode(user.getId());
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .cacheControl(CacheControl.noStore())
        .body(image);
  }

  private String extractToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return "";
    }
    String prefix = "Bearer ";
    return authorization.startsWith(prefix) ? authorization.substring(prefix.length()).trim() : authorization.trim();
  }
}
