package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.PasswordLoginRequest;
import com.zhixi.backend.dto.RegisterRequest;
import com.zhixi.backend.dto.ResetPasswordRequest;
import com.zhixi.backend.dto.SmsLoginRequest;
import com.zhixi.backend.dto.SmsSendRequest;
import com.zhixi.backend.dto.UserProfileUpdateRequest;
import com.zhixi.backend.dto.WechatBindMobileRequest;
import com.zhixi.backend.dto.WechatMiniappLoginRequest;
import com.zhixi.backend.dto.WechatMiniappQrConfirmRequest;
import com.zhixi.backend.dto.WechatMockScanRequest;
import com.zhixi.backend.config.UploadStorageSupport;
import com.zhixi.backend.model.User;
import com.zhixi.backend.service.UserAuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif");
  private final UserAuthService userAuthService;
  private final UploadStorageSupport uploadStorageSupport;

  @Value("${app.wechat.web-success-redirect-uri:https://mashishi.com}")
  private String wechatWebSuccessRedirectUri;

  public AuthController(UserAuthService userAuthService, UploadStorageSupport uploadStorageSupport) {
    this.userAuthService = userAuthService;
    this.uploadStorageSupport = uploadStorageSupport;
  }

  @PostMapping("/register")
  public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok(userAuthService.registerUser(request));
  }

  @PostMapping("/login")
  public ApiResponse<Map<String, Object>> loginByPassword(@Valid @RequestBody PasswordLoginRequest request) {
    return ApiResponse.ok(userAuthService.loginByPassword(request));
  }

  @PostMapping("/wechat-miniapp/login")
  public ApiResponse<Map<String, Object>> loginByMiniapp(@Valid @RequestBody WechatMiniappLoginRequest request) {
    return ApiResponse.ok(userAuthService.loginByMiniapp(request));
  }

  @PostMapping("/wechat-miniapp/qr/create")
  public ApiResponse<Map<String, Object>> createWechatMiniappQr() {
    return ApiResponse.ok(userAuthService.createMiniappQrLoginSession());
  }

  @GetMapping(value = "/wechat-miniapp/qr/image/{scene}", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getWechatMiniappQrImage(@PathVariable String scene) {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(userAuthService.getMiniappQrLoginImage(scene));
  }

  @GetMapping("/wechat-miniapp/qr/status/{scene}")
  public ApiResponse<Map<String, Object>> queryWechatMiniappQrStatus(@PathVariable String scene) {
    return ApiResponse.ok(userAuthService.queryWechatQrStatus(scene));
  }

  @PostMapping("/wechat-miniapp/qr/confirm")
  public ApiResponse<Map<String, Object>> confirmWechatMiniappQr(
      @Valid @RequestBody WechatMiniappQrConfirmRequest request
  ) {
    return ApiResponse.ok(userAuthService.confirmMiniappQrLogin(request));
  }

  @PostMapping("/sms/send")
  public ApiResponse<Map<String, Object>> sendSms(@Valid @RequestBody SmsSendRequest request) {
    return ApiResponse.ok(userAuthService.sendSmsCode(request.getPhone(), request.getScene()));
  }

  @PostMapping("/sms/login")
  public ApiResponse<Map<String, Object>> loginBySms(@Valid @RequestBody SmsLoginRequest request) {
    return ApiResponse.ok(userAuthService.loginBySms(request));
  }

  @PostMapping("/password/reset")
  public ApiResponse<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    return ApiResponse.ok(userAuthService.resetPassword(request));
  }

  @PostMapping("/wechat/qr/create")
  public ApiResponse<Map<String, Object>> createWechatQr() {
    return ApiResponse.ok(userAuthService.createWechatQrSession());
  }

  @GetMapping("/wechat/qr/status/{scene}")
  public ApiResponse<Map<String, Object>> queryQrStatus(@PathVariable String scene) {
    return ApiResponse.ok(userAuthService.queryWechatQrStatus(scene));
  }

  @PostMapping("/wechat/mock-scan")
  public ApiResponse<Void> mockScan(@Valid @RequestBody WechatMockScanRequest request) {
    userAuthService.mockScanWechat(request.getScene(), request.getOpenid(), request.getNickname());
    return ApiResponse.ok(null);
  }

  @PostMapping("/wechat/bind-mobile")
  public ApiResponse<Map<String, Object>> bindWechatMobile(@Valid @RequestBody WechatBindMobileRequest request) {
    return ApiResponse.ok(userAuthService.bindWechatMobile(request));
  }

  @GetMapping(value = "/wechat/callback", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> wechatCallback(
      @RequestParam String code,
      @RequestParam(name = "state", required = false) String scene
  ) {
    try {
      Map<String, Object> authData = userAuthService.handleWechatWebCallback(code, scene);
      Object tokenValue =REMOTE_BACKUP_REDACTED
      String token =REMOTE_BACKUP_REDACTED
      return ResponseEntity.ok(wechatCallbackHtml(
          "寰俊鐧诲綍鎴愬姛",
          "姝ｅ湪杩斿洖缃戠珯骞跺悓姝ョ櫥褰曠姸鎬侊紝璇风◢鍊?..",
          resolveWechatSuccessRedirectUri(),
          true,
          token
      ));
    } catch (Exception ex) {
      return ResponseEntity.badRequest().body(wechatCallbackHtml(
          "寰俊鐧诲綍澶辫触",
          ex.getMessage(),
          resolveWechatSuccessRedirectUri(),
          false,
          ""
      ));
    }
  }

  @PostMapping("/profile/avatar")
  public ApiResponse<Map<String, String>> uploadProfileAvatar(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file,
      HttpServletRequest request
  ) {
    userAuthService.getUserByToken(extractToken(authorization));

    if (file == null || file.isEmpty()) {
      throw new BusinessException("璇烽€夋嫨澶村儚鍥剧墖");
    }
    if (file.getSize() > 2 * 1024 * 1024) {
      throw new BusinessException("澶村儚鍥剧墖澶у皬涓嶈兘瓒呰繃2MB");
    }

    String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
    if (ext == null || ext.isBlank()) {
      ext = "png";
    }
    ext = ext.toLowerCase();
    String contentType = file.getContentType();
    if ((contentType == null || !contentType.startsWith("image/")) && !IMAGE_EXTENSIONS.contains(ext)) {
      throw new BusinessException("浠呮敮鎸佸浘鐗囨枃浠?);
    }
    String filename = "user-" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    Path dir = uploadStorageSupport.resolveUploadDir();
    Path target = dir.resolve(filename);
    try {
      Files.createDirectories(dir);
      try (InputStream inputStream = file.getInputStream()) {
        java.nio.file.Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      log.error("澶村儚涓婁紶澶辫触锛岀洰鏍囪矾寰勶細{}", target, e);
      throw new BusinessException("澶村儚涓婁紶澶辫触锛? + e.getMessage());
    }

    String scheme = request.getHeader("X-Forwarded-Proto");
    if (scheme == null || scheme.isBlank()) {
      scheme = request.getScheme();
    }
    String host = request.getHeader("Host");
    if (host == null || host.isBlank()) {
      host = request.getServerName();
    }
    String url = scheme + "://" + host + "/api/uploads/" + filename;

    Map<String, String> data = new HashMap<>();
    data.put("url", url);
    data.put("filename", filename);
    return ApiResponse.ok(data);
  }

  @PostMapping("/profile")
  public ApiResponse<Map<String, Object>> updateProfile(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody(required = false) UserProfileUpdateRequest request
  ) {
    String nickname = request == null ? "" : request.getNickname();
    String avatarUrl = request == null ? "" : request.getAvatarUrl();
    return ApiResponse.ok(userAuthService.updateCurrentUserProfile(extractToken(authorization), nickname, avatarUrl));
  }

  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
    User user = userAuthService.getUserByToken(extractToken(authorization));
    Map<String, Object> data = new HashMap<>();
    data.put("id", user.getId());
    data.put("userId", user.getId());
    data.put("phone", publicPhone(user.getPhone()));
    data.put("nickname", userAuthService.getSharedDisplayNickname(user.getId(), user.getNickname()));
    data.put("inviteCode", user.getInviteCode());
    data.put("avatarUrl", userAuthService.getUserAvatarUrl(user.getId()));
    data.put("wechatWebOpenid", userAuthService.getWechatOpenidBySource(user.getId(), "WEB"));
    data.put("wechatMiniappOpenid", userAuthService.getWechatOpenidBySource(user.getId(), "MINIAPP"));
    return ApiResponse.ok(data);
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
    userAuthService.logout(extractToken(authorization));
    return ApiResponse.ok(null);
  }

  private String extractToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return "";
    }
    String prefix = "Bearer ";
    return authorization.startsWith(prefix) ? authorization.substring(prefix.length()).trim() : authorization.trim();
  }

  private String wechatCallbackHtml(String title, String message, String redirectUrl, boolean autoRedirect, String token) {
    String safeRedirectUrl = buildWechatRedirectUrl(redirectUrl, token);
    String metaRefresh = autoRedirect
        ? "<meta http-equiv=\"refresh\" content=\"1;url=" + escapeHtml(safeRedirectUrl) + "\">"
        : "";
    String redirectScript = autoRedirect
        ? "<script>setTimeout(function(){window.location.replace('" + escapeJsString(safeRedirectUrl) + "');}, 1000);</script>"
        : "";
    String redirectLink = autoRedirect
        ? "<a class=\"action\" href=\"" + escapeHtml(safeRedirectUrl) + "\">杩斿洖缃戠珯棣栭〉</a>"
        : "";

    return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
        + "<title>" + escapeHtml(title) + "</title>"
        + metaRefresh
        + "<style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
        + "display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:24px;color:#1f2937;background:#f8fafc}"
        + ".box{text-align:center;line-height:1.7;max-width:360px;padding:28px 24px;border-radius:18px;background:#fff;box-shadow:0 14px 40px rgba(15,23,42,.08)}"
        + ".title{font-size:22px;font-weight:700;margin-bottom:8px}.msg{color:#6b7280;margin-bottom:18px}.action{display:inline-block;padding:10px 18px;border-radius:999px;background:#07c160;color:#fff;text-decoration:none;font-weight:600}</style>"
        + "</head><body><div class=\"box\"><div class=\"title\">"
        + escapeHtml(title)
        + "</div><div class=\"msg\">"
        + escapeHtml(message)
        + "</div>"
        + redirectLink
        + "</div>"
        + redirectScript
        + "</body></html>";
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private String escapeJsString(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("</", "<\\/");
  }

  private String resolveWechatSuccessRedirectUri() {
    if (wechatWebSuccessRedirectUri == null || wechatWebSuccessRedirectUri.isBlank()) {
      return "https://mashishi.com";
    }
    return wechatWebSuccessRedirectUri.trim();
  }

  private String buildWechatRedirectUrl(String redirectUrl, String token) {
    String safeRedirectUrl = (redirectUrl == null || redirectUrl.isBlank())
        ? "https://mashishi.com"
        : redirectUrl.trim();
    if (token == null || token.isBlank()) {
      return safeRedirectUrl;
    }

    String encodedToken =REMOTE_BACKUP_REDACTED
    String tokenFragment =REMOTE_BACKUP_REDACTED
    if (safeRedirectUrl.contains("#")) {
      return safeRedirectUrl + "&" + tokenFragment;
    }
    return safeRedirectUrl + "#" + tokenFragment;
  }

  private String publicPhone(String phone) {
    if (phone == null) {
      return "";
    }
    if (phone.startsWith("wxmp_") || phone.startsWith("wxweb_")) {
      return "";
    }
    return phone;
  }
}

