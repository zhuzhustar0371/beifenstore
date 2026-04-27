package com.zhixi.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZhixiBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(ZhixiBackendApplication.class, args);
  }
}
