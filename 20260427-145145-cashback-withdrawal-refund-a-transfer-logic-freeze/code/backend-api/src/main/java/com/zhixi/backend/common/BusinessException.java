package com.zhixi.backend.common;

public class BusinessException extends RuntimeException {
  public BusinessException(String message) {
    super(message);
  }
}
