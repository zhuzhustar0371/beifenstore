package com.zhixi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixi.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MiniappAccessTokenService {
  private static final Logger log = LoggerFactory.getLogger(MiniappAccessTokenService.class);

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Value("${app.wechat.miniapp.app-id:}")
  private String miniappAppId;

  @Value("${app.wechat.miniapp.app-secret:}")
  private String miniappAppSecret;

  private volatile String accessToken;
  private volatile LocalDateTime accessTokenExpiresAt;

  public MiniappAccessTokenService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String getAccessToken() {
    return loadAccessToken(false);
  }

  public String forceRefreshAccessToken() {
    return loadAccessToken(true);
  }

  public void clearCachedAccessToken() {
    synchronized (this) {
      accessToken = null;
      accessTokenExpiresAt = null;
    }
  }

  private String loadAccessToken(boolean forceRefresh) {
    validateMiniappConfig();
    if (!forceRefresh && hasValidAccessToken()) {
      return accessToken;
    }

    synchronized (this) {
      if (!forceRefresh && hasValidAccessToken()) {
        return accessToken;
      }
      return fetchStableAccessToken(forceRefresh);
    }
  }

  private String fetchStableAccessToken(boolean forceRefresh) {
    Map<String, Object> requestBody = new LinkedHashMap<>();
    requestBody.put("grant_type", "client_credential");
    requestBody.put("appid", miniappAppId.trim());
    requestBody.put("secret", miniappAppSecret.trim());
    requestBody.put("force_refresh", forceRefresh);

    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/cgi-bin/stable_token"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("Failed to get stable miniapp access_token: HTTP " + response.statusCode());
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> payload = objectMapper.readValue(response.body(), Map.class);
      int errCode = numberValue(payload.get("errcode"));
      if (errCode != 0) {
        throw new BusinessException(resolveWechatError(payload, "Failed to get stable miniapp access_token"));
      }

      String nextAccessToken = stringValue(payload.get("access_token"));
      if (nextAccessToken.isBlank()) {
        throw new BusinessException("Failed to get stable miniapp access_token: access_token is empty");
      }

      long expiresIn = numberValue(payload.get("expires_in"));
      if (expiresIn <= 0) {
        expiresIn = 7200L;
      }

      accessToken = nextAccessToken;
      accessTokenExpiresAt = LocalDateTime.now().plusSeconds(Math.max(expiresIn - 300L, 60L));
      if (forceRefresh) {
        log.info("Force refreshed stable miniapp access token");
      }
      return accessToken;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("Failed to get stable miniapp access_token");
    }
  }

  private boolean hasValidAccessToken() {
    return accessToken != null
        && !accessToken.isBlank()
        && accessTokenExpiresAt != null
        && accessTokenExpiresAt.isAfter(LocalDateTime.now());
  }

  private void validateMiniappConfig() {
    if (miniappAppId == null || miniappAppId.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_ID is not configured");
    }
    if (miniappAppSecret == null || miniappAppSecret.isBlank()) {
      throw new BusinessException("WECHAT_MINIAPP_APP_SECRET is not configured");
    }
  }

  private int numberValue(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }

  private String stringValue(Object value) {
    return value instanceof String text ? text.trim() : "";
  }

  private String resolveWechatError(Map<String, Object> payload, String fallbackMessage) {
    Object errCode = payload.get("errcode");
    Object errMsg = payload.get("errmsg");
    if (errCode != null || errMsg != null) {
      return fallbackMessage + ": errcode=" + String.valueOf(errCode) + ", errmsg=" + String.valueOf(errMsg);
    }
    return fallbackMessage;
  }
}
