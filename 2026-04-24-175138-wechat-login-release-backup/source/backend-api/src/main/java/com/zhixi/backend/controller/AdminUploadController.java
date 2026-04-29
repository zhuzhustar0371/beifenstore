package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.config.UploadStorageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/uploads")
public class AdminUploadController {
  private static final Logger log = LoggerFactory.getLogger(AdminUploadController.class);
  private final UploadStorageSupport uploadStorageSupport;

  public AdminUploadController(UploadStorageSupport uploadStorageSupport) {
    this.uploadStorageSupport = uploadStorageSupport;
  }

  @PostMapping("/image")
  public ApiResponse<Map<String, String>> uploadImage(
      @RequestParam("file") MultipartFile file,
      HttpServletRequest request
  ) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请选择要上传的图片");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new BusinessException("仅支持图片文件");
    }
    if (file.getSize() > 2 * 1024 * 1024) {
      throw new BusinessException("图片大小不能超过2MB");
    }
    String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
    if (ext == null || ext.isBlank()) {
      ext = "png";
    }
    String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
    Path dir = uploadStorageSupport.resolveUploadDir();
    Path target = dir.resolve(filename);
    try {
      Files.createDirectories(dir);
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      log.error("图片上传失败，目标路径：{}", target, e);
      throw new BusinessException("图片上传失败：" + e.getMessage());
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
}
