package com.zhixi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.mapper.InviteRelationMapper;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InviteService {
  private static final String MINIAPP_INVITE_PAGE = "pages/index/index";

  private final InviteRelationMapper inviteRelationMapper;
  private final UserService userService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final Object tokenLock =REMOTE_BACKUP_REDACTED

  @Value("${app.wechat.miniapp.app-id:}")
  private String wechatMiniappAppId;

  @Value("${app.wechat.miniapp.app-secret:}")
  private String wechatMiniappAppSecret;

  private volatile String miniappAccessToken;
  private volatile LocalDateTime miniappAccessTokenExpiresAt;

  public InviteService(InviteRelationMapper inviteRelationMapper, UserService userService, ObjectMapper objectMapper) {
    this.inviteRelationMapper = inviteRelationMapper;
    this.userService = userService;
    this.objectMapper = objectMapper;
  }

  public List<InviteRelation> listByInviter(Long inviterId) {
    userService.getUser(inviterId);
    return inviteRelationMapper.findByInviterId(inviterId);
  }

  public byte[] createMiniappInviteCode(Long inviterId) {
    User user = userService.getUser(inviterId);
    if (user.getInviteCode() == null || user.getInviteCode().isBlank()) {
      throw new BusinessException("\u5f53\u524d\u8d26\u53f7\u6682\u65e0\u9080\u8bf7\u7801\uff0c\u65e0\u6cd5\u751f\u6210\u9080\u8bf7\u4e8c\u7ef4\u7801");
    }

    String accessToken =REMOTE_BACKUP_REDACTED
    Map<String, Object> payload = new HashMap<>();
    payload.put("scene", "inviterId=" + inviterId);
    payload.put("page", MINIAPP_INVITE_PAGE);
    payload.put("check_path", false);
    payload.put("env_version", "release");
    payload.put("width", 430);
    return requestMiniappCode(accessToken, payload);
  }

  private String getMiniappAccessToken() {
    validateMiniappConfig();
    if (hasValidMiniappAccessToken()) {
      return miniappAccessToken;
    }

    synchronized (tokenLock) {
      if (hasValidMiniappAccessToken()) {
        return miniappAccessToken;
      }

      Map<String, Object> data = fetchMiniappAccessToken();
      Object token =REMOTE_BACKUP_REDACTED
      if (!(token instanceof String tokenValue) || tokenValue.isBlank()) {
        throw new BusinessException("\u5fae\u4fe1 access_token \u83b7\u53d6\u5931\u8d25");
      }

      long expiresIn = 7200L;
      Object expires = data.get("expires_in");
      if (expires instanceof Number number) {
        expiresIn = number.longValue();
      }

      miniappAccessToken =REMOTE_BACKUP_REDACTED
      miniappAccessTokenExpiresAt =REMOTE_BACKUP_REDACTED
      return miniappAccessToken;
    }
  }

  private Map<String, Object> fetchMiniappAccessToken() {
    String url = "https://api.weixin.qq.com/cgi-bin/token"
        + "?grant_type=client_credential"
        + "&appid=" + URLEncoder.encode(wechatMiniappAppId, StandardCharsets.UTF_8)
        + "&secret=" + URLEncoder.encode(wechatMiniappAppSecret, StandardCharsets.UTF_8);

    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("\u5fae\u4fe1 access_token \u63a5\u53e3\u8fd4\u56de\u5f02\u5e38\u72b6\u6001: " + response.statusCode());
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> payload = objectMapper.readValue(response.body(), Map.class);
      if (payload.get("errcode") instanceof Number code && code.intValue() != 0) {
        throw new BusinessException(resolveWechatError(payload, "\u5fae\u4fe1 access_token \u83b7\u53d6\u5931\u8d25"));
      }
      return payload;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("\u5fae\u4fe1 access_token \u83b7\u53d6\u5931\u8d25");
    }
  }

  private byte[] requestMiniappCode(String accessToken, Map<String, Object> payload) {
    try {
      String body = objectMapper.writeValueAsString(payload);
      String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token="
          + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u63a5\u53e3\u8fd4\u56de\u5f02\u5e38\u72b6\u6001: " + response.statusCode());
      }

      byte[] bodyBytes = response.body();
      if (bodyBytes == null || bodyBytes.length == 0) {
        throw new BusinessException("\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u751f\u6210\u5931\u8d25\uff0c\u63a5\u53e3\u8fd4\u56de\u4e3a\u7a7a");
      }

      if (looksLikeJson(response, bodyBytes)) {
        @SuppressWarnings("unchecked")
        Map<String, Object> error = objectMapper.readValue(bodyBytes, Map.class);
        throw new BusinessException(resolveWechatError(error, "\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u751f\u6210\u5931\u8d25"));
      }

      return bodyBytes;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u751f\u6210\u5931\u8d25");
    }
  }

  private boolean hasValidMiniappAccessToken() {
    return miniappAccessToken != null
        && !miniappAccessToken.isBlank()
        && miniappAccessTokenExpiresAt != null
        && miniappAccessTokenExpiresAt.isAfter(LocalDateTime.now());
  }

  private boolean looksLikeJson(HttpResponse<byte[]> response, byte[] bodyBytes) {
    String contentType = response.headers().firstValue("Content-Type").orElse("");
    if (contentType.contains("application/json") || contentType.contains("text/plain")) {
      return true;
    }

    for (byte bodyByte : bodyBytes) {
      char ch = (char) bodyByte;
      if (Character.isWhitespace(ch)) {
        continue;
      }
      return ch == '{';
    }
    return false;
  }

  private void validateMiniappConfig() {
    if (wechatMiniappAppId == null || wechatMiniappAppId.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_ID \u672a\u914d\u7f6e");
    }
    if (wechatMiniappAppSecret == null || wechatMiniappAppSecret.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_SECRET \u672a\u914d\u7f6e");
    }
  }

  private String resolveWechatError(Map<String, Object> payload, String fallback) {
    Object errCode = payload.get("errcode");
    Object errMsg = payload.get("errmsg");
    if (errCode != null || errMsg != null) {
      return fallback + ": errcode=" + String.valueOf(errCode) + ", errmsg=" + String.valueOf(errMsg);
    }
    return fallback;
  }
}

