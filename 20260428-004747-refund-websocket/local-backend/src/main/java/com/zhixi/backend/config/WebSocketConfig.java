package com.zhixi.backend.config;

import com.zhixi.backend.service.AdminAuthService;
import com.zhixi.backend.service.WithdrawalWebSocketService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final WithdrawalWebSocketService withdrawalWebSocketService;
  private final AdminAuthService adminAuthService;

  public WebSocketConfig(WithdrawalWebSocketService withdrawalWebSocketService, AdminAuthService adminAuthService) {
    this.withdrawalWebSocketService = withdrawalWebSocketService;
    this.adminAuthService = adminAuthService;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(withdrawalWebSocketService, "/ws/withdrawals")
        .setAllowedOrigins("*")
        .addInterceptors(new HandshakeInterceptor() {
          @Override
          public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                         WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String query = request.getURI().getQuery();
            String token = extractParam(query, "token");
            if (token == null || token.isBlank()) {
              return false;
            }
            try {
              adminAuthService.getAdminByToken(token);
              return true;
            } catch (Exception e) {
              return false;
            }
          }

          @Override
          public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                     WebSocketHandler wsHandler, Exception exception) {
          }
        });
  }

  private String extractParam(String query, String key) {
    if (query == null) return null;
    for (String pair : query.split("&")) {
      int idx = pair.indexOf("=");
      if (idx > 0 && key.equals(pair.substring(0, idx))) {
        return pair.substring(idx + 1);
      }
    }
    return null;
  }
}
