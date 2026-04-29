package com.zhixi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.common.PasswordCodec;
import com.zhixi.backend.dto.PasswordLoginRequest;
import com.zhixi.backend.dto.RegisterRequest;
import com.zhixi.backend.dto.ResetPasswordRequest;
import com.zhixi.backend.dto.SmsLoginRequest;
import com.zhixi.backend.dto.WechatBindMobileRequest;
import com.zhixi.backend.dto.WechatMiniappLoginRequest;
import com.zhixi.backend.dto.WechatMiniappQrConfirmRequest;
import com.zhixi.backend.mapper.SmsLoginCodeMapper;
import com.zhixi.backend.mapper.UserSessionMapper;
import com.zhixi.backend.mapper.UserWechatAuthMapper;
import com.zhixi.backend.mapper.WechatQrSessionMapper;
import com.zhixi.backend.model.SmsLoginCode;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.UserSession;
import com.zhixi.backend.model.UserWechatAuth;
import com.zhixi.backend.model.WechatQrSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class UserAuthService {
  private static final Logger log = LoggerFactory.getLogger(UserAuthService.class);
  private static final String SMS_SCENE_REGISTER = "REGISTER";
  private static final String SMS_SCENE_LOGIN = "LOGIN";
  private static final String SMS_SCENE_RESET_PASSWORD = "RESET_PASSWORD";
  private static final String WECHAT_SOURCE_MINIAPP = "MINIAPP";
  private static final String WECHAT_SOURCE_WEB = "WEB";
  private static final String WEB_LOGIN_MINIAPP_PAGE = "pages/web-login/web-login";

  private final SmsLoginCodeMapper smsLoginCodeMapper;
  private final UserSessionMapper userSessionMapper;
  private final UserWechatAuthMapper userWechatAuthMapper;
  private final WechatQrSessionMapper wechatQrSessionMapper;
  private final UserService userService;
  private final CaptchaService captchaService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Value("${app.user-session.expires-hours:24}")
  private int sessionHours;

  @Value("${app.sms.code-expire-minutes:5}")
  private int smsCodeExpireMinutes;

  @Value("${app.sms.skip-verify:false}")
  private boolean smsSkipVerify;

  @Value("${app.wechat.web-app-id:}")
  private String wechatWebAppId;

  @Value("${app.wechat.web-app-secret:}")
  private String wechatWebAppSecret;

  @Value("${app.wechat.redirect-uri:}")
  private String wechatRedirectUri;

  @Value("${app.wechat.miniapp.app-id:}")
  private String wechatMiniappAppId;

  @Value("${app.wechat.miniapp.app-secret:}")
  private String wechatMiniappAppSecret;

  @Value("${app.sms.tencent.secret-id:}")
  private String tencentSmsSecretId;

  @Value("${app.sms.tencent.secret-key:}")
  private String tencentSmsSecretKey;

  @Value("${app.sms.tencent.sdk-app-id:}")
  private String tencentSmsSdkAppId;

  @Value("${app.sms.tencent.sign-name:}")
  private String tencentSmsSignName;

  @Value("${app.sms.tencent.template-id:}")
  private String tencentSmsTemplateId;

  @Value("${app.sms.tencent.reset-template-id:}")
  private String tencentResetSmsTemplateId;

  @Value("${app.sms.tencent.region:ap-guangzhou}")
  private String tencentSmsRegion;

  private volatile String miniappAccessToken;
  private volatile LocalDateTime miniappAccessTokenExpiresAt;

  public UserAuthService(
      SmsLoginCodeMapper smsLoginCodeMapper,
      UserSessionMapper userSessionMapper,
      UserWechatAuthMapper userWechatAuthMapper,
      WechatQrSessionMapper wechatQrSessionMapper,
      UserService userService,
      CaptchaService captchaService,
      ObjectMapper objectMapper
  ) {
    this.smsLoginCodeMapper = smsLoginCodeMapper;
    this.userSessionMapper = userSessionMapper;
    this.userWechatAuthMapper = userWechatAuthMapper;
    this.wechatQrSessionMapper = wechatQrSessionMapper;
    this.userService = userService;
    this.captchaService = captchaService;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> sendSmsCode(String phone, String scene) {
    String normalizedScene = normalizeSmsScene(scene);
    validateSmsSendTarget(phone, normalizedScene);
    String code = String.valueOf(100000 + new Random().nextInt(900000));

    if (!smsSkipVerify) {
      sendTencentSms(phone, code, normalizedScene);
    }

    SmsLoginCode item = new SmsLoginCode();
    item.setPhone(phone);
    item.setCode(code);
    item.setScene(normalizedScene);
    item.setUsed(false);
    item.setExpiresAt(LocalDateTime.now().plusMinutes(smsCodeExpireMinutes));
    smsLoginCodeMapper.insert(item);

    Map<String, Object> data = new HashMap<>();
    data.put("phone", phone);
    data.put("scene", normalizedScene);
    data.put("expireMinutes", smsCodeExpireMinutes);
    if (smsSkipVerify) {
      data.put("debugCode", code);
    }
    return data;
  }

  @Transactional
  public Map<String, Object> loginBySms(SmsLoginRequest request) {
    verifySmsCodeWithFallback(request.getPhone(), request.getCode(), SMS_SCENE_LOGIN, SMS_SCENE_REGISTER);

    User user = userService.findByPhone(request.getPhone());
    if (user == null) {
      user = userService.createFromPhone(request.getPhone(), request.getNickname(), request.getInviteCode());
    } else {
      userService.bindInviterIfNeeded(user.getId(), request.getInviteCode());
      user = userService.getUser(user.getId());
    }

    String token = createUserSession(user.getId(), "SMS");
    return authResponse(user, token);
  }

  @Transactional
  public Map<String, Object> registerUser(RegisterRequest request) {
    captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

    if (!smsSkipVerify) {
      verifySmsCode(request.getPhone(), request.getSmsCode(), SMS_SCENE_REGISTER);
    }

    User user = userService.register(request);
    String token = createUserSession(user.getId(), "PASSWORD");
    return authResponse(user, token);
  }

  @Transactional
  public Map<String, Object> loginByPassword(PasswordLoginRequest request) {
    captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

    User user = userService.findByPhone(request.getPhone());
    if (user == null) {
      throw new BusinessException("用户不存在");
    }
    if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
      throw new BusinessException("当前账号未设置密码，请先使用短信登录");
    }

    String inputHash = PasswordCodec.sha256(request.getPassword());
    if (!inputHash.equals(user.getPasswordHash())) {
      throw new BusinessException("密码错误");
    }

    String token = createUserSession(user.getId(), "PASSWORD");
    return authResponse(user, token);
  }

  @Transactional
  public Map<String, Object> resetPassword(ResetPasswordRequest request) {
    captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

    if (!smsSkipVerify) {
      verifySmsCode(request.getPhone(), request.getSmsCode(), SMS_SCENE_RESET_PASSWORD);
    }

    userService.resetPasswordByPhone(request.getPhone(), request.getNewPassword());

    Map<String, Object> data = new HashMap<>();
    data.put("phone", request.getPhone());
    data.put("reset", true);
    return data;
  }

  public Map<String, Object> createWechatQrSession() {
    validateWechatWebConfig();

    String scene = UUID.randomUUID().toString().replace("-", "");
    WechatQrSession session = new WechatQrSession();
    session.setScene(scene);
    session.setStatus("PENDING");
    session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
    wechatQrSessionMapper.insert(session);

    String wechatUrl = "https://open.weixin.qq.com/connect/qrconnect?appid=" + wechatWebAppId
        + "&redirect_uri=" + URLEncoder.encode(wechatRedirectUri, StandardCharsets.UTF_8)
        + "&response_type=code&scope=snsapi_login&state=" + scene + "#wechat_redirect";

    Map<String, Object> data = new HashMap<>();
    data.put("scene", scene);
    data.put("status", "PENDING");
    data.put("wechatAuthUrl", wechatUrl);
    data.put("qrImageUrl", "");
    data.put("expireSeconds", 300);
    return data;
  }

  public Map<String, Object> queryWechatQrStatus(String scene) {
    WechatQrSession session = wechatQrSessionMapper.findByScene(scene);
    if (session == null) {
      throw new BusinessException("微信登录会话不存在");
    }
    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      Map<String, Object> expired = new HashMap<>();
      expired.put("status", "EXPIRED");
      return expired;
    }

    Map<String, Object> data = new HashMap<>();
    data.put("status", session.getStatus());
    if ("AUTHORIZED".equals(session.getStatus())) {
      User user = userService.getUser(session.getUserId());
      data.putAll(authResponse(user, session.getToken()));
    }
    if ("SCANNED".equals(session.getStatus())) {
      data.put("openid", session.getOpenid());
      data.put("nickname", session.getNickname());
    }
    return data;
  }

  public Map<String, Object> createMiniappQrLoginSession() {
    validateMiniappConfig();

    String scene = UUID.randomUUID().toString().replace("-", "");
    WechatQrSession session = new WechatQrSession();
    session.setScene(scene);
    session.setStatus("PENDING");
    session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
    wechatQrSessionMapper.insert(session);

    Map<String, Object> data = new HashMap<>();
    data.put("scene", scene);
    data.put("status", "PENDING");
    data.put("qrImageUrl", "/api/auth/wechat-miniapp/qr/image/" + scene);
    data.put("expireSeconds", 300);
    return data;
  }

  public byte[] getMiniappQrLoginImage(String scene) {
    WechatQrSession session = wechatQrSessionMapper.findByScene(scene);
    if (session == null) {
      throw new BusinessException("WeChat mini-program login session does not exist");
    }
    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("WeChat mini-program login session expired");
    }
    return requestMiniappWebLoginCode(scene);
  }

  public void mockScanWechat(String scene, String openid, String nickname) {
    WechatQrSession session = wechatQrSessionMapper.findByScene(scene);
    if (session == null) {
      throw new BusinessException("微信登录会话不存在");
    }
    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("微信登录会话已过期");
    }
    wechatQrSessionMapper.markScanned(scene, "SCANNED", openid, nickname);
  }

  @Transactional
  public Map<String, Object> bindWechatMobile(WechatBindMobileRequest request) {
    WechatQrSession session = wechatQrSessionMapper.findByScene(request.getScene());
    if (session == null || !"SCANNED".equals(session.getStatus())) {
      throw new BusinessException("微信登录状态无效，请重新扫码");
    }
    verifySmsCode(request.getPhone(), request.getCode(), SMS_SCENE_REGISTER);

    User user = userService.findByPhone(request.getPhone());
    if (user == null) {
      user = userService.createFromPhone(request.getPhone(), request.getNickname(), request.getInviteCode());
    } else {
      userService.bindInviterIfNeeded(user.getId(), request.getInviteCode());
      user = userService.getUser(user.getId());
    }

    UserWechatAuth auth = userWechatAuthMapper.findByOpenid(session.getOpenid());
    if (auth == null) {
      UserWechatAuth row = new UserWechatAuth();
      row.setUserId(user.getId());
      row.setSourceType(WECHAT_SOURCE_WEB);
      row.setOpenid(session.getOpenid());
      row.setNickname(session.getNickname());
      userWechatAuthMapper.insert(row);
    }
    syncWechatProfile(user.getId(), normalizeText(request.getNickname(), session.getNickname()), "");

    String token = createUserSession(user.getId(), "WECHAT");
    wechatQrSessionMapper.markAuthorized(request.getScene(), user.getId(), token);
    return authResponse(user, token);
  }

  @Transactional
  public Map<String, Object> loginByMiniapp(WechatMiniappLoginRequest request) {
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw new BusinessException("Missing mini-program login code");
    }

    WechatIdentity identity = exchangeMiniappIdentity(request.getCode());
    boolean isNewUser = isFirstMiniappLogin(identity);
    String nickname = normalizeText(request.getNickName(), "");
    String avatarUrl = normalizeText(request.getAvatarUrl(), "");
    User user = resolveMiniappUser(identity, nickname, avatarUrl, request.getInviterId());

    String token = createUserSession(user.getId(), "MINIAPP");
    Map<String, Object> data = authResponse(user, token);
    data.put("isNewUser", isNewUser);
    data.put("needProfileCompletion", isNewUser);
    return data;
  }

  @Transactional
  public Map<String, Object> confirmMiniappQrLogin(WechatMiniappQrConfirmRequest request) {
    WechatQrSession session = wechatQrSessionMapper.findByScene(request.getScene());
    if (session == null) {
      throw new BusinessException("WeChat mini-program login session does not exist");
    }
    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("WeChat mini-program login session expired");
    }
    if ("AUTHORIZED".equals(session.getStatus())) {
      User user = userService.getUser(session.getUserId());
      return authResponse(user, session.getToken());
    }
    if (!"PENDING".equals(session.getStatus()) && !"SCANNED".equals(session.getStatus())) {
      throw new BusinessException("WeChat mini-program login session is not available");
    }

    WechatIdentity identity = exchangeMiniappIdentity(request.getCode());
    String nickname = normalizeText(request.getNickName(), "");
    String avatarUrl = normalizeText(request.getAvatarUrl(), "");
    User user = resolveMiniappUser(identity, nickname, avatarUrl, request.getInviterId());

    String token = createUserSession(user.getId(), "MINIAPP_QR");
    wechatQrSessionMapper.markScanned(request.getScene(), "SCANNED", identity.openid(), nickname.isBlank() ? ("微信用户" + identity.shortOpenid()) : nickname);
    wechatQrSessionMapper.markAuthorized(request.getScene(), user.getId(), token);
    return authResponse(user, token);
  }

  @Transactional
  public Map<String, Object> handleWechatWebCallback(String code, String scene) {
    if (code == null || code.isBlank()) {
      throw new BusinessException("Missing WeChat OAuth code");
    }
    if (scene == null || scene.isBlank()) {
      throw new BusinessException("Missing WeChat login scene");
    }

    WechatQrSession session = wechatQrSessionMapper.findByScene(scene);
    if (session == null) {
      throw new BusinessException("WeChat login session does not exist");
    }
    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("WeChat login session expired");
    }

    WechatWebProfile profile = exchangeWechatWebProfile(code);
    User user = resolveWechatUser(
        WECHAT_SOURCE_WEB,
        profile.openid(),
        profile.unionid(),
        normalizeText(profile.nickname(), "瀵邦喕淇婇悽銊﹀煕" + profile.shortOpenid()),
        normalizeText(profile.avatarUrl(), ""),
        "wxweb"
    );

    String token = createUserSession(user.getId(), "WECHAT_WEB");
    wechatQrSessionMapper.markScanned(scene, "SCANNED", profile.openid(), profile.nickname());
    wechatQrSessionMapper.markAuthorized(scene, user.getId(), token);
    return authResponse(user, token);
  }

  public User getUserByToken(String token) {
    if (token == null || token.isBlank()) {
      throw new BusinessException("请先登录");
    }
    userSessionMapper.deleteExpired();
    UserSession session = userSessionMapper.findByToken(token);
    if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("登录状态已过期，请重新登录");
    }
    return userService.getUser(session.getUserId());
  }

  public void logout(String token) {
    if (token == null || token.isBlank()) {
      return;
    }
    userSessionMapper.deleteByToken(token);
  }

  public String getUserAvatarUrl(Long userId) {
    return resolveWechatProfile(userId, "").avatarUrl();
  }

  public String getDisplayNickname(Long userId, String fallback) {
    return getSharedDisplayNickname(userId, fallback);
  }

  public String getWechatOpenidBySource(Long userId, String sourceType) {
    if (userId == null || sourceType == null || sourceType.isBlank()) {
      return "";
    }

    UserWechatAuth auth = userWechatAuthMapper.findByUserIdAndSource(
        userId,
        sourceType.trim().toUpperCase(Locale.ROOT)
    );
    if (auth == null) {
      return "";
    }
    return normalizeText(auth.getOpenid(), "");
  }

  public String getSharedDisplayNickname(Long userId, String fallback) {
    return resolveWechatProfile(userId, fallback).nickname();
  }

  private WechatProfileSnapshot resolveWechatProfile(Long userId, String fallbackNickname) {
    String normalizedFallbackNickname = normalizeText(fallbackNickname, "");
    if (userId == null) {
      return new WechatProfileSnapshot(
          normalizedFallbackNickname.isBlank() ? "微信用户" : normalizedFallbackNickname,
          ""
      );
    }

    UserWechatAuth miniappAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_MINIAPP);
    UserWechatAuth webAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_WEB);
    UserWechatAuth legacyAuth = miniappAuth == null && webAuth == null ? userWechatAuthMapper.findByUserId(userId) : null;

    String nickname = resolveSharedNickname(normalizedFallbackNickname, miniappAuth, webAuth, legacyAuth);
    String avatarUrl = resolveSharedAvatarUrl("", miniappAuth, webAuth, legacyAuth);
    return new WechatProfileSnapshot(nickname, avatarUrl);
  }

  private String resolveSharedNickname(
      String preferredNickname,
      UserWechatAuth miniappAuth,
      UserWechatAuth webAuth,
      UserWechatAuth legacyAuth
  ) {
    String normalizedPreferred = normalizeText(preferredNickname, "");
    String miniappNickname = normalizeText(miniappAuth == null ? "" : miniappAuth.getNickname(), "");
    String webNickname = normalizeText(webAuth == null ? "" : webAuth.getNickname(), "");
    String legacyNickname = normalizeText(legacyAuth == null ? "" : legacyAuth.getNickname(), "");

    if (!normalizedPreferred.isBlank() && !isDefaultWechatNickname(normalizedPreferred)) {
      return normalizedPreferred;
    }
    if (!miniappNickname.isBlank() && !isDefaultWechatNickname(miniappNickname)) {
      return miniappNickname;
    }
    if (!webNickname.isBlank() && !isDefaultWechatNickname(webNickname)) {
      return webNickname;
    }
    if (!legacyNickname.isBlank() && !isDefaultWechatNickname(legacyNickname)) {
      return legacyNickname;
    }
    if (!normalizedPreferred.isBlank()) {
      return normalizedPreferred;
    }
    if (!miniappNickname.isBlank()) {
      return miniappNickname;
    }
    if (!webNickname.isBlank()) {
      return webNickname;
    }
    if (!legacyNickname.isBlank()) {
      return legacyNickname;
    }
    return "微信用户";
  }

  private String resolveSharedAvatarUrl(
      String preferredAvatarUrl,
      UserWechatAuth miniappAuth,
      UserWechatAuth webAuth,
      UserWechatAuth legacyAuth
  ) {
    String normalizedPreferred = normalizeText(preferredAvatarUrl, "");
    if (!normalizedPreferred.isBlank()) {
      return normalizedPreferred;
    }

    String miniappAvatar = normalizeText(miniappAuth == null ? "" : miniappAuth.getAvatarUrl(), "");
    if (!miniappAvatar.isBlank()) {
      return miniappAvatar;
    }

    String webAvatar = normalizeText(webAuth == null ? "" : webAuth.getAvatarUrl(), "");
    if (!webAvatar.isBlank()) {
      return webAvatar;
    }

    String legacyAvatar = normalizeText(legacyAuth == null ? "" : legacyAuth.getAvatarUrl(), "");
    if (!legacyAvatar.isBlank()) {
      return legacyAvatar;
    }
    return "";
  }

  private void syncWechatProfile(Long userId, String preferredNickname, String preferredAvatarUrl) {
    if (userId == null) {
      return;
    }

    User user = userService.getUser(userId);
    UserWechatAuth miniappAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_MINIAPP);
    UserWechatAuth webAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_WEB);
    UserWechatAuth legacyAuth = miniappAuth == null && webAuth == null ? userWechatAuthMapper.findByUserId(userId) : null;

    String resolvedNickname = resolveSharedNickname(preferredNickname, miniappAuth, webAuth, legacyAuth);
    String resolvedAvatarUrl = resolveSharedAvatarUrl(preferredAvatarUrl, miniappAuth, webAuth, legacyAuth);
    String currentNickname = normalizeText(user.getNickname(), "");
    String normalizedNickname = normalizeText(resolvedNickname, "");

    if (!normalizedNickname.isBlank() && !normalizedNickname.equals(currentNickname)) {
      userService.updateNickname(userId, normalizedNickname);
    }

    applyWechatProfileUpdate(miniappAuth, WECHAT_SOURCE_MINIAPP, normalizedNickname, resolvedAvatarUrl);
    applyWechatProfileUpdate(webAuth, WECHAT_SOURCE_WEB, normalizedNickname, resolvedAvatarUrl);
    if (legacyAuth != null && miniappAuth == null && webAuth == null) {
      applyWechatProfileUpdate(legacyAuth, legacyAuth.getSourceType(), normalizedNickname, resolvedAvatarUrl);
    }
  }

  private void applyWechatProfileUpdate(
      UserWechatAuth auth,
      String sourceType,
      String nickname,
      String avatarUrl
  ) {
    if (auth == null) {
      return;
    }

    boolean changed = false;
    String normalizedSourceType = normalizeText(sourceType, auth.getSourceType());
    if (!normalizedSourceType.isBlank() && !normalizedSourceType.equalsIgnoreCase(normalizeText(auth.getSourceType(), ""))) {
      auth.setSourceType(normalizedSourceType.toUpperCase(Locale.ROOT));
      changed = true;
    }

    String normalizedNickname = normalizeText(nickname, "");
    if (!normalizedNickname.isBlank() && !normalizedNickname.equals(normalizeText(auth.getNickname(), ""))) {
      auth.setNickname(normalizedNickname);
      changed = true;
    }

    String normalizedAvatarUrl = normalizeText(avatarUrl, "");
    if (!normalizedAvatarUrl.isBlank() && !normalizedAvatarUrl.equals(normalizeText(auth.getAvatarUrl(), ""))) {
      auth.setAvatarUrl(normalizedAvatarUrl);
      changed = true;
    }

    if (changed) {
      userWechatAuthMapper.updateProfile(auth);
    }
  }

  private record WechatProfileSnapshot(String nickname, String avatarUrl) {}

  @Transactional
  public Map<String, Object> updateCurrentUserProfile(String token, String nickname, String avatarUrl) {
    User user = getUserByToken(token);
    String normalizedNickname = normalizeText(nickname, "");
    String normalizedAvatarUrl = normalizeText(avatarUrl, "");
    if (normalizedNickname.isBlank() && normalizedAvatarUrl.isBlank()) {
      throw new BusinessException("请输入昵称或上传头像");
    }

    if (!normalizedNickname.isBlank()) {
      userService.updateNickname(user.getId(), normalizedNickname);
      user = userService.getUser(user.getId());
    }

    UserWechatAuth auth = userWechatAuthMapper.findByUserIdAndSource(user.getId(), WECHAT_SOURCE_MINIAPP);
    if (auth == null) {
      auth = userWechatAuthMapper.findByUserId(user.getId());
    }
    if (auth != null) {
      auth.setNickname(normalizedNickname.isBlank() ? normalizeText(auth.getNickname(), user.getNickname()) : normalizedNickname);
      auth.setAvatarUrl(normalizedAvatarUrl.isBlank() ? normalizeText(auth.getAvatarUrl(), "") : normalizedAvatarUrl);
      auth.setSourceType(auth.getSourceType() == null || auth.getSourceType().isBlank() ? WECHAT_SOURCE_MINIAPP : auth.getSourceType());
      try {
        userWechatAuthMapper.updateProfile(auth);
      } catch (Exception ex) {
        log.warn("Failed to update full WeChat profile, fallback to nickname-only update. userId={}", user.getId(), ex);
        if (!normalizedNickname.isBlank()) {
          try {
            userWechatAuthMapper.updateNicknameOnly(auth.getId(), normalizedNickname);
          } catch (Exception fallbackEx) {
            log.warn("Fallback nickname-only update failed. userId={}", user.getId(), fallbackEx);
          }
        }
      }
    } else if (user.getMiniappOpenid() != null && !user.getMiniappOpenid().isBlank()) {
      String finalNickname = normalizedNickname.isBlank() ? user.getNickname() : normalizedNickname;
      try {
        UserWechatAuth row = new UserWechatAuth();
        row.setUserId(user.getId());
        row.setSourceType(WECHAT_SOURCE_MINIAPP);
        row.setOpenid(user.getMiniappOpenid());
        row.setUnionid(null);
        row.setNickname(finalNickname);
        row.setAvatarUrl(normalizedAvatarUrl);
        userWechatAuthMapper.insert(row);
      } catch (Exception ex) {
        log.warn("Failed to insert full WeChat profile, fallback to legacy insert. userId={}", user.getId(), ex);
        try {
          userWechatAuthMapper.insertLegacy(user.getId(), user.getMiniappOpenid(), finalNickname);
        } catch (Exception fallbackEx) {
          log.warn("Fallback legacy insert failed. userId={}", user.getId(), fallbackEx);
        }
      }
    }

    syncWechatProfile(user.getId(), normalizedNickname, normalizedAvatarUrl);
    return authResponse(userService.getUser(user.getId()), token);
  }

  private void sendTencentSms(String phone, String code, String scene) {
    try {
      com.tencentcloudapi.common.Credential cred =
          new com.tencentcloudapi.common.Credential(tencentSmsSecretId, tencentSmsSecretKey);
      com.tencentcloudapi.common.profile.HttpProfile httpProfile =
          new com.tencentcloudapi.common.profile.HttpProfile();
      httpProfile.setEndpoint("sms.tencentcloudapi.com");
      com.tencentcloudapi.common.profile.ClientProfile clientProfile =
          new com.tencentcloudapi.common.profile.ClientProfile();
      clientProfile.setHttpProfile(httpProfile);

      com.tencentcloudapi.sms.v20210111.SmsClient client =
          new com.tencentcloudapi.sms.v20210111.SmsClient(cred, tencentSmsRegion, clientProfile);
      com.tencentcloudapi.sms.v20210111.models.SendSmsRequest req =
          new com.tencentcloudapi.sms.v20210111.models.SendSmsRequest();
      req.setPhoneNumberSet(new String[]{"+86" + phone});
      req.setSmsSdkAppId(tencentSmsSdkAppId);
      req.setSignName(tencentSmsSignName);
      req.setTemplateId(resolveSmsTemplateId(scene));
      req.setTemplateParamSet(new String[]{code, String.valueOf(smsCodeExpireMinutes)});

      com.tencentcloudapi.sms.v20210111.models.SendSmsResponse res = client.SendSms(req);
      if (res.getSendStatusSet() != null && res.getSendStatusSet().length > 0) {
        String codeStatus = res.getSendStatusSet()[0].getCode();
        if (!"Ok".equalsIgnoreCase(codeStatus)) {
          throw new BusinessException(resolveSmsProviderMessage(codeStatus, res.getSendStatusSet()[0].getMessage()));
        }
      }
    } catch (com.tencentcloudapi.common.exception.TencentCloudSDKException e) {
      throw new BusinessException(resolveSmsProviderMessage(null, e.getMessage()));
    }
  }

  private String createUserSession(Long userId, String loginType) {
    userSessionMapper.deleteExpired();
    String token = UUID.randomUUID().toString().replace("-", "");
    UserSession session = new UserSession();
    session.setUserId(userId);
    session.setToken(token);
    session.setLoginType(loginType);
    session.setExpiresAt(LocalDateTime.now().plusHours(sessionHours));
    userSessionMapper.insert(session);
    return token;
  }

  private Map<String, Object> authResponse(User user, String token) {
    Map<String, Object> data = new HashMap<>();
    data.put("token", token);
    data.put("userId", user.getId());
    data.put("phone", publicPhone(user.getPhone()));
    data.put("nickname", getSharedDisplayNickname(user.getId(), user.getNickname()));
    data.put("inviteCode", user.getInviteCode());
    data.put("avatarUrl", getUserAvatarUrl(user.getId()));
    data.put("wechatWebOpenid", getWechatOpenidBySource(user.getId(), WECHAT_SOURCE_WEB));
    data.put("wechatMiniappOpenid", getWechatOpenidBySource(user.getId(), WECHAT_SOURCE_MINIAPP));
    return data;
  }

  private boolean isFirstMiniappLogin(WechatIdentity identity) {
    if (identity == null) {
      return false;
    }
    if (userService.findByMiniappOpenid(identity.openid()) != null) {
      return false;
    }
    if (hasExistingWechatAuthUser(userWechatAuthMapper.findBySourceAndOpenid(WECHAT_SOURCE_MINIAPP, identity.openid()))) {
      return false;
    }
    if (hasExistingWechatAuthUser(userWechatAuthMapper.findByOpenid(identity.openid()))) {
      return false;
    }

    String unionid = normalizeText(identity.unionid(), "");
    if (!unionid.isBlank() && hasExistingWechatAuthUser(userWechatAuthMapper.findByUnionid(unionid))) {
      return false;
    }
    return true;
  }

  private boolean hasExistingWechatAuthUser(UserWechatAuth auth) {
    if (auth == null) {
      return false;
    }
    try {
      userService.getUser(auth.getUserId());
      return true;
    } catch (BusinessException ex) {
      log.warn(
          "Wechat auth points to missing user, authId={}, userId={}, openid={}, message={}",
          auth.getId(),
          auth.getUserId(),
          auth.getOpenid(),
          ex.getMessage()
      );
      return false;
    }
  }

  private User resolveMiniappUser(
      WechatIdentity identity,
      String nickname,
      String avatarUrl,
      Long inviterId
  ) {
    User user = userService.findByMiniappOpenid(identity.openid());
    if (user != null) {
      UserWechatAuth existingAuth = userWechatAuthMapper.findBySourceAndOpenid(WECHAT_SOURCE_MINIAPP, identity.openid());
      String mergedNickname = resolveNicknameForExistingUser(nickname, existingAuth, user, identity);
      String mergedAvatarUrl = resolveAvatarUrlForExistingUser(avatarUrl, existingAuth);

      upsertWechatAuth(
          user.getId(),
          WECHAT_SOURCE_MINIAPP,
          identity.openid(),
          identity.unionid(),
          mergedNickname,
          mergedAvatarUrl
      );

      if (!nickname.isBlank()) {
        userService.updateNickname(user.getId(), nickname);
      }
      syncWechatProfile(user.getId(), mergedNickname, mergedAvatarUrl);
      user = userService.getUser(user.getId());
    } else {
      String createNickname = nickname.isBlank() ? ("微信用户" + identity.shortOpenid()) : nickname;
      user = resolveWechatUser(
          WECHAT_SOURCE_MINIAPP,
          identity.openid(),
          identity.unionid(),
          createNickname,
          avatarUrl,
          "wxmp"
      );
    }

    if (user.getMiniappOpenid() == null || user.getMiniappOpenid().isBlank()) {
      userService.updateMiniappOpenid(user.getId(), identity.openid());
      user = userService.getUser(user.getId());
    }

    if (inviterId != null) {
      try {
        userService.bindInviterIfNeeded(user.getId(), userService.getUser(inviterId).getInviteCode());
        user = userService.getUser(user.getId());
      } catch (BusinessException ex) {
        log.warn(
            "Miniapp login ignored invalid inviter, userId={}, inviterId={}, message={}",
            user.getId(),
            inviterId,
            ex.getMessage()
        );
      }
    }
    return user;
  }

  private User resolveWechatUser(
      String sourceType,
      String openid,
      String unionid,
      String nickname,
      String avatarUrl,
      String accountPrefix
  ) {
    UserWechatAuth auth = userWechatAuthMapper.findBySourceAndOpenid(sourceType, openid);
    if (auth == null && unionid != null && !unionid.isBlank()) {
      auth = userWechatAuthMapper.findByUnionid(unionid);
    }

    User user = null;
    if (auth != null) {
      try {
        user = userService.getUser(auth.getUserId());
      } catch (BusinessException ex) {
        log.warn(
            "Wechat auth will be rebound because linked user is missing, authId={}, userId={}, sourceType={}, openid={}, message={}",
            auth.getId(),
            auth.getUserId(),
            auth.getSourceType(),
            auth.getOpenid(),
            ex.getMessage()
        );
      }
    }

    if (user == null) {
      user = userService.createFromPhone(generateWechatAccountKey(accountPrefix, openid), nickname, null);
    }

    if (auth != null && (auth.getUserId() == null || !auth.getUserId().equals(user.getId()))) {
      userWechatAuthMapper.updateUserId(auth.getId(), user.getId());
      auth.setUserId(user.getId());
    }

    upsertWechatAuth(user.getId(), sourceType, openid, unionid, nickname, avatarUrl);
    syncWechatProfile(user.getId(), nickname, avatarUrl);
    return userService.getUser(user.getId());
  }

  private void upsertWechatAuth(
      Long userId,
      String sourceType,
      String openid,
      String unionid,
      String nickname,
      String avatarUrl
  ) {
    UserWechatAuth auth = userWechatAuthMapper.findBySourceAndOpenid(sourceType, openid);
    if (auth == null) {
      auth = userWechatAuthMapper.findByOpenid(openid);
    }

    if (auth == null) {
      UserWechatAuth row = new UserWechatAuth();
      row.setUserId(userId);
      row.setSourceType(sourceType);
      row.setOpenid(openid);
      row.setUnionid(unionid);
      row.setNickname(nickname);
      row.setAvatarUrl(avatarUrl);
      userWechatAuthMapper.insert(row);
      return;
    }

    auth.setSourceType(sourceType);
    if (auth.getUserId() == null || !auth.getUserId().equals(userId)) {
      userWechatAuthMapper.updateUserId(auth.getId(), userId);
      auth.setUserId(userId);
    }
    auth.setUnionid(unionid);
    auth.setNickname(nickname);
    auth.setAvatarUrl(avatarUrl);
    userWechatAuthMapper.updateProfile(auth);
  }

  private String resolveNicknameForExistingUser(
      String inputNickname,
      UserWechatAuth existingAuth,
      User user,
      WechatIdentity identity
  ) {
    String normalizedInput = normalizeText(inputNickname, "");
    String authNickname = existingAuth == null ? "" : normalizeText(existingAuth.getNickname(), "");
    String userNickname = user == null ? "" : normalizeText(user.getNickname(), "");

    if (!normalizedInput.isBlank()) {
      return normalizedInput;
    }
    if (!authNickname.isBlank() && !isDefaultWechatNickname(authNickname)) {
      return authNickname;
    }
    if (!userNickname.isBlank() && !isDefaultWechatNickname(userNickname)) {
      return userNickname;
    }
    if (!authNickname.isBlank()) {
      return authNickname;
    }
    if (!userNickname.isBlank()) {
      return userNickname;
    }
    return "微信用户" + identity.shortOpenid();
  }

  private String resolveAvatarUrlForExistingUser(String inputAvatarUrl, UserWechatAuth existingAuth) {
    String normalizedInput = normalizeText(inputAvatarUrl, "");
    if (!normalizedInput.isBlank()) {
      return normalizedInput;
    }
    if (existingAuth == null) {
      return "";
    }
    return normalizeText(existingAuth.getAvatarUrl(), "");
  }

  private void validateSmsSendTarget(String phone, String scene) {
    if (SMS_SCENE_RESET_PASSWORD.equals(scene) && userService.findByPhone(phone) == null) {
      throw new BusinessException("用户不存在");
    }
  }

  private String normalizeSmsScene(String scene) {
    if (scene == null || scene.isBlank()) {
      return SMS_SCENE_REGISTER;
    }
    String normalized = scene.trim().toUpperCase(Locale.ROOT);
    if (SMS_SCENE_REGISTER.equals(normalized)
        || SMS_SCENE_LOGIN.equals(normalized)
        || SMS_SCENE_RESET_PASSWORD.equals(normalized)) {
      return normalized;
    }
    throw new BusinessException("短信场景不支持");
  }

  private String resolveSmsTemplateId(String scene) {
    if (SMS_SCENE_RESET_PASSWORD.equals(scene)
        && tencentResetSmsTemplateId != null
        && !tencentResetSmsTemplateId.isBlank()) {
      return tencentResetSmsTemplateId;
    }
    return tencentSmsTemplateId;
  }

  private String resolveSmsProviderMessage(String statusCode, String statusMessage) {
    String normalizedCode = statusCode == null ? "" : statusCode.trim().toLowerCase(Locale.ROOT);
    String normalizedMessage = statusMessage == null ? "" : statusMessage.trim().toLowerCase(Locale.ROOT);

    if (normalizedCode.contains("limit")
        || normalizedMessage.contains("every day exceeds the upper limit")
        || normalizedMessage.contains("daily limit")) {
      return "短信发送次数已达当日上限，请明天再试";
    }

    if (normalizedCode.contains("frequency")
        || normalizedMessage.contains("frequency limit")
        || normalizedMessage.contains("too many")
        || normalizedMessage.contains("too frequent")) {
      return "短信发送过于频繁，请稍后重试";
    }

    if (normalizedMessage.contains("template")) {
      return "短信模板配置错误，请联系管理员";
    }

    if (normalizedMessage.contains("signature") || normalizedMessage.contains("sign")) {
      return "短信签名配置错误，请联系管理员";
    }

    if (statusMessage != null && !statusMessage.isBlank()) {
      return "短信发送失败: " + statusMessage;
    }
    return "短信发送失败，请稍后重试";
  }

  private WechatIdentity exchangeMiniappIdentity(String code) {
    if (wechatMiniappAppId == null || wechatMiniappAppId.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_ID is not configured");
    }
    if (wechatMiniappAppSecret == null || wechatMiniappAppSecret.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_SECRET is not configured");
    }

    String url = "https://api.weixin.qq.com/sns/jscode2session"
        + "?appid=" + URLEncoder.encode(wechatMiniappAppId, StandardCharsets.UTF_8)
        + "&secret=" + URLEncoder.encode(wechatMiniappAppSecret, StandardCharsets.UTF_8)
        + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
        + "&grant_type=authorization_code";

    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn(
            "Miniapp jscode2session http error, appId={}, code={}, status={}, body={}",
            wechatMiniappAppId,
            maskMiniappCode(code),
            response.statusCode(),
            abbreviateForLog(response.body())
        );
        throw new BusinessException("微信登录服务返回异常状态: " + response.statusCode());
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> payload = objectMapper.readValue(response.body(), Map.class);
      Object errCode = payload.get("errcode");
      if (errCode instanceof String text && !text.isBlank() && !"0".equals(text)) {
        String errMsg = String.valueOf(payload.getOrDefault("errmsg", "unknown"));
        log.warn(
            "Miniapp jscode2session business error, appId={}, code={}, errcode={}, errmsg={}, body={}",
            wechatMiniappAppId,
            maskMiniappCode(code),
            text,
            errMsg,
            abbreviateForLog(response.body())
        );
        throw new BusinessException("小程序登录失败: " + errMsg);
      }
      if (errCode instanceof Number number && number.intValue() != 0) {
        String errMsg = String.valueOf(payload.getOrDefault("errmsg", "unknown"));
        log.warn(
            "Miniapp jscode2session business error, appId={}, code={}, errcode={}, errmsg={}, body={}",
            wechatMiniappAppId,
            maskMiniappCode(code),
            number.intValue(),
            errMsg,
            abbreviateForLog(response.body())
        );
        throw new BusinessException("小程序登录失败: " + errMsg);
      }

      Object openid = payload.get("openid");
      if (!(openid instanceof String openidValue) || openidValue.isBlank()) {
        log.warn(
            "Miniapp jscode2session missing openid, appId={}, code={}, body={}",
            wechatMiniappAppId,
            maskMiniappCode(code),
            abbreviateForLog(response.body())
        );
        throw new BusinessException("小程序登录未返回 openid");
      }
      return new WechatIdentity(
          openidValue,
          stringValue(payload.get("unionid")),
          stringValue(payload.get("session_key"))
      );
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Miniapp jscode2session request failed, appId={}, code={}",
          wechatMiniappAppId,
          maskMiniappCode(code),
          ex
      );
      throw new BusinessException("小程序登录换取 openid 失败");
    }
  }

  private byte[] requestMiniappWebLoginCode(String scene) {
    try {
      String accessToken = getMiniappAccessToken();
      String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit"
          + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

      Map<String, Object> body = new HashMap<>();
      body.put("scene", scene);
      body.put("page", WEB_LOGIN_MINIAPP_PAGE);
      body.put("check_path", false);
      body.put("width", 280);

      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("Mini-program QR code request failed: HTTP " + response.statusCode());
      }

      byte[] payload = response.body();
      if (looksLikeJson(payload)) {
        @SuppressWarnings("unchecked")
        Map<String, Object> error = objectMapper.readValue(new String(payload, StandardCharsets.UTF_8), Map.class);
        Object errCode = error.get("errcode");
        if (errCode instanceof Number number && number.intValue() == 0) {
          return payload;
        }
        throw new BusinessException("Mini-program QR code request failed: "
            + error.getOrDefault("errmsg", "unknown"));
      }
      return payload;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("Mini-program QR code request failed");
    }
  }

  private String getMiniappAccessToken() {
    validateMiniappConfig();
    if (hasValidMiniappAccessToken()) {
      return miniappAccessToken;
    }

    synchronized (this) {
      if (hasValidMiniappAccessToken()) {
        return miniappAccessToken;
      }

      try {
        String url = "https://api.weixin.qq.com/cgi-bin/token"
            + "?grant_type=client_credential"
            + "&appid=" + URLEncoder.encode(wechatMiniappAppId, StandardCharsets.UTF_8)
            + "&secret=" + URLEncoder.encode(wechatMiniappAppSecret, StandardCharsets.UTF_8);
        Map<String, Object> payload = getWechatJson(url, "Mini-program access_token failed");
        String accessToken = stringValue(payload.get("access_token"));
        if (accessToken.isBlank()) {
          throw new BusinessException("Mini-program access_token is empty");
        }
        Object expiresInValue = payload.get("expires_in");
        long expiresIn = expiresInValue instanceof Number number ? number.longValue() : 7200L;
        miniappAccessToken = accessToken;
        miniappAccessTokenExpiresAt = LocalDateTime.now().plusSeconds(Math.max(expiresIn - 300L, 60L));
        return miniappAccessToken;
      } catch (BusinessException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BusinessException("Mini-program access_token failed");
      }
    }
  }

  private boolean hasValidMiniappAccessToken() {
    return miniappAccessToken != null
        && !miniappAccessToken.isBlank()
        && miniappAccessTokenExpiresAt != null
        && miniappAccessTokenExpiresAt.isAfter(LocalDateTime.now());
  }

  private boolean looksLikeJson(byte[] payload) {
    if (payload == null) {
      return false;
    }
    for (byte item : payload) {
      if (!Character.isWhitespace((char) item)) {
        return item == '{' || item == '[';
      }
    }
    return false;
  }

  private String generateMiniappPlaceholderPhone(String openid) {
    long seed = Integer.toUnsignedLong(openid.hashCode());
    for (int attempt = 0; attempt < 20; attempt++) {
      long value = (seed + attempt * 104729L) % 1_000_000_000L;
      String candidate = "19" + String.format("%09d", value);
      if (userService.findByPhone(candidate) == null) {
        return candidate;
      }
    }
    return "19" + String.format("%09d", System.currentTimeMillis() % 1_000_000_000L);
  }

  private WechatWebProfile exchangeWechatWebProfile(String code) {
    validateWechatWebConfig();

    String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token"
        + "?appid=" + URLEncoder.encode(wechatWebAppId, StandardCharsets.UTF_8)
        + "&secret=" + URLEncoder.encode(wechatWebAppSecret, StandardCharsets.UTF_8)
        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
        + "&grant_type=authorization_code";

    try {
      Map<String, Object> tokenPayload = getWechatJson(tokenUrl, "WeChat web access token failed");
      String accessToken = stringValue(tokenPayload.get("access_token"));
      String openid = stringValue(tokenPayload.get("openid"));
      String unionid = stringValue(tokenPayload.get("unionid"));
      if (accessToken.isBlank() || openid.isBlank()) {
        throw new BusinessException("WeChat web login did not return access_token or openid");
      }

      String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo"
          + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
          + "&openid=" + URLEncoder.encode(openid, StandardCharsets.UTF_8)
          + "&lang=zh_CN";
      Map<String, Object> userPayload = getWechatJson(userInfoUrl, "WeChat web userinfo failed");
      String profileUnionid = stringValue(userPayload.get("unionid"));
      return new WechatWebProfile(
          openid,
          profileUnionid.isBlank() ? unionid : profileUnionid,
          stringValue(userPayload.get("nickname")),
          stringValue(userPayload.get("headimgurl"))
      );
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("WeChat web login failed");
    }
  }

  private Map<String, Object> getWechatJson(String url, String fallbackMessage) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new BusinessException(fallbackMessage + ": HTTP " + response.statusCode());
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = objectMapper.readValue(response.body(), Map.class);
    Object errCode = payload.get("errcode");
    if (errCode instanceof Number number && number.intValue() != 0) {
      throw new BusinessException(fallbackMessage + ": " + payload.getOrDefault("errmsg", "unknown"));
    }
    if (errCode instanceof String text && !text.isBlank() && !"0".equals(text)) {
      throw new BusinessException(fallbackMessage + ": " + payload.getOrDefault("errmsg", "unknown"));
    }
    return payload;
  }

  private void validateMiniappConfig() {
    if (wechatMiniappAppId == null || wechatMiniappAppId.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_ID is not configured");
    }
    if (wechatMiniappAppSecret == null || wechatMiniappAppSecret.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_SECRET is not configured");
    }
  }

  private void validateWechatWebConfig() {
    if (wechatWebAppId == null || wechatWebAppId.isBlank()) {
      throw new BusinessException("WECHAT_WEB_APP_ID is not configured");
    }
    if (wechatWebAppSecret == null || wechatWebAppSecret.isBlank()) {
      throw new BusinessException("WECHAT_WEB_APP_SECRET is not configured");
    }
    if (wechatRedirectUri == null || wechatRedirectUri.isBlank()) {
      throw new BusinessException("WECHAT_WEB_REDIRECT_URI is not configured");
    }
  }

  private String generateWechatAccountKey(String prefix, String openid) {
    String normalizedPrefix = (prefix == null || prefix.isBlank()) ? "wx" : prefix;
    String source = UUID.nameUUIDFromBytes(openid.getBytes(StandardCharsets.UTF_8))
        .toString()
        .replace("-", "");
    return normalizedPrefix + "_" + source.substring(0, Math.min(24, source.length()));
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

  private boolean isDefaultWechatNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return true;
    }
    String value = nickname.trim();
    return value.startsWith("微信用户") || value.startsWith("寰俊鐢ㄦ埛") || value.startsWith("瀵邦喕淇婇悽銊﹀煕");
  }

  private String normalizeText(String value, String fallback) {
    if (value == null || value.trim().isEmpty()) {
      return fallback;
    }
    return value.trim();
  }

  private String stringValue(Object value) {
    return value instanceof String text ? text.trim() : "";
  }

  private String maskMiniappCode(String code) {
    if (code == null || code.isBlank()) {
      return "-";
    }
    if (code.length() <= 8) {
      return code;
    }
    return code.substring(0, 4) + "..." + code.substring(code.length() - 4);
  }

  private String abbreviateForLog(String value) {
    if (value == null || value.isBlank()) {
      return "-";
    }
    return value.length() <= 500 ? value : value.substring(0, 500) + "...";
  }

  private record WechatIdentity(String openid, String unionid, String sessionKey) {
    private String shortOpenid() {
      return openid.substring(0, Math.min(4, openid.length()));
    }
  }

  private record WechatWebProfile(String openid, String unionid, String nickname, String avatarUrl) {
    private String shortOpenid() {
      return openid.substring(0, Math.min(4, openid.length()));
    }
  }

  private void verifySmsCode(String phone, String inputCode, String scene) {
    SmsLoginCode code = smsLoginCodeMapper.findLatest(phone, inputCode, scene);
    if (code == null || Boolean.TRUE.equals(code.getUsed()) || code.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("验证码无效或已过期");
    }
    smsLoginCodeMapper.markUsed(code.getId());
  }

  private void verifySmsCodeWithFallback(String phone, String inputCode, String primaryScene, String fallbackScene) {
    try {
      verifySmsCode(phone, inputCode, primaryScene);
    } catch (BusinessException ex) {
      verifySmsCode(phone, inputCode, fallbackScene);
    }
  }
}
