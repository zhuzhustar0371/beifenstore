package com.zhixi.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class UploadStorageSupport {
  private static final Logger log = LoggerFactory.getLogger(UploadStorageSupport.class);

  private final String configuredUploadDir;
  private volatile Path resolvedUploadDir;

  public UploadStorageSupport(@Value("${app.upload.dir:uploads}") String configuredUploadDir) {
    this.configuredUploadDir = StringUtils.hasText(configuredUploadDir) ? configuredUploadDir.trim() : "uploads";
  }

  public Path resolveUploadDir() {
    Path cached = resolvedUploadDir;
    if (cached != null) {
      return cached;
    }
    synchronized (this) {
      if (resolvedUploadDir != null) {
        return resolvedUploadDir;
      }
      resolvedUploadDir = chooseWritableUploadDir();
      return resolvedUploadDir;
    }
  }

  public String resolveResourceLocation() {
    return resolveUploadDir().toAbsolutePath().toUri().toString();
  }

  public Path resolveFile(String filename) {
    return resolveUploadDir().resolve(filename);
  }

  private Path chooseWritableUploadDir() {
    Set<Path> candidates = new LinkedHashSet<>();
    addCandidate(candidates, configuredUploadDir);
    addCandidate(candidates, Paths.get("uploads").toString());
    addCandidate(candidates, Paths.get("public", "uploads").toString());
    addCandidate(candidates, Paths.get(System.getProperty("java.io.tmpdir"), "zhixi-backend-uploads").toString());

    IOException lastException = null;
    for (Path candidate : candidates) {
      Path normalized = candidate.toAbsolutePath().normalize();
      try {
        Files.createDirectories(normalized);
        if (Files.isDirectory(normalized) && Files.isWritable(normalized)) {
          log.info("Using upload directory: {}", normalized);
          return normalized;
        }
        lastException = new IOException("目录不可写: " + normalized);
      } catch (IOException ex) {
        lastException = ex;
        log.warn("Failed to prepare upload directory {}: {}", normalized, ex.getMessage());
      }
    }

    if (lastException != null) {
      throw new IllegalStateException("无法创建可写上传目录", lastException);
    }
    throw new IllegalStateException("无法创建可写上传目录");
  }

  private void addCandidate(Set<Path> candidates, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    try {
      candidates.add(Paths.get(value.trim()));
    } catch (Exception ex) {
      log.warn("Invalid upload directory candidate '{}': {}", value, ex.getMessage());
    }
  }
}
