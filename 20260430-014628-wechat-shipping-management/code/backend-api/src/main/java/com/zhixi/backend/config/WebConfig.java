package com.zhixi.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AdminAuthInterceptor adminAuthInterceptor;
  private final UploadStorageSupport uploadStorageSupport;

  public WebConfig(AdminAuthInterceptor adminAuthInterceptor, UploadStorageSupport uploadStorageSupport) {
    this.adminAuthInterceptor = adminAuthInterceptor;
    this.uploadStorageSupport = uploadStorageSupport;
  }

  @Value("${app.cors.origins:http://localhost:5173}")
  private String origins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] allowed = origins.split(",");
    registry.addMapping("/**")
        .allowedOrigins(allowed)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(adminAuthInterceptor)
        .addPathPatterns("/api/admin/**");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/api/uploads/**")
        .addResourceLocations(uploadStorageSupport.resolveResourceLocation());
  }
}
