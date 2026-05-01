package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.dto.MiniappInviteRecordVO;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.User;
import com.zhixi.backend.service.InviteService;
import com.zhixi.backend.service.UserAuthService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invites")
public class InviteController {
  private final InviteService inviteService;
  private final UserAuthService userAuthService;

  public InviteController(InviteService inviteService, UserAuthService userAuthService) {
    this.inviteService = inviteService;
    this.userAuthService = userAuthService;
  }

  @GetMapping("/{userId}")
  public ApiResponse<List<InviteRelation>> list(@PathVariable Long userId) {
    return ApiResponse.ok(inviteService.listByInviter(userId));
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
