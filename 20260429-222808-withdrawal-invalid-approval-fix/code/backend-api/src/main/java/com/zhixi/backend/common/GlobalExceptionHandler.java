package com.zhixi.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
    log.warn(
        "Business API exception, method={}, path={}, clientIp={}, userAgent={}, message={}",
        request == null ? "-" : request.getMethod(),
        request == null ? "-" : request.getRequestURI(),
        resolveClientIp(request),
        abbreviate(request == null ? "" : request.getHeader("User-Agent")),
        ex.getMessage()
    );
    return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getAllErrors().stream().findFirst()
        .map(error -> error.getDefaultMessage())
        .orElse("参数校验失败");
    return ResponseEntity.badRequest().body(ApiResponse.fail(msg));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
    log.error("Unhandled API exception", ex);
    return ResponseEntity.internalServerError().body(ApiResponse.fail("服务开小差了，请稍后重试"));
  }

  private static String resolveClientIp(HttpServletRequest request) {
    if (request == null) {
      return "-";
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int commaIndex = forwarded.indexOf(',');
      return commaIndex >= 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
    }
    return request.getRemoteAddr();
  }

  private static String abbreviate(String value) {
    if (value == null || value.isBlank()) {
      return "-";
    }
    return value.length() <= 200 ? value : value.substring(0, 200) + "...";
  }
}
