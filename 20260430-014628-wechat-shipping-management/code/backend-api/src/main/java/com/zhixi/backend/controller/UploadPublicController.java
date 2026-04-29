package com.zhixi.backend.controller;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.config.UploadStorageSupport;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/uploads")
public class UploadPublicController {
  private final UploadStorageSupport uploadStorageSupport;

  public UploadPublicController(UploadStorageSupport uploadStorageSupport) {
    this.uploadStorageSupport = uploadStorageSupport;
  }

  @GetMapping("/{filename}")
  public ResponseEntity<Resource> view(@PathVariable String filename) {
    if (!StringUtils.hasText(filename) || filename.contains("..") || filename.contains("/")) {
      throw new BusinessException("文件不存在");
    }
    Path file = uploadStorageSupport.resolveFile(filename);
    FileSystemResource resource = new FileSystemResource(file);
    if (!resource.exists() || !resource.isReadable()) {
      throw new BusinessException("文件不存在");
    }
    String ext = StringUtils.getFilenameExtension(filename);
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if ("png".equalsIgnoreCase(ext)) mediaType = MediaType.IMAGE_PNG;
    if ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)) mediaType = MediaType.IMAGE_JPEG;
    if ("webp".equalsIgnoreCase(ext)) mediaType = MediaType.valueOf("image/webp");
    if ("gif".equalsIgnoreCase(ext)) mediaType = MediaType.IMAGE_GIF;
    return ResponseEntity.ok().contentType(mediaType).body(resource);
  }
}
