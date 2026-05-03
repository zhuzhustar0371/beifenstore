package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {
  private static final int WIDTH = 150;
  private static final int HEIGHT = 48;
  private static final int CODE_LENGTH = 4;
  private static final long EXPIRE_MS = 5 * 60 * 1000;
  private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();
  private final Random random = new Random();

  public Map<String, String> generate() {
    String id = UUID.randomUUID().toString().replace("-", "");
    String code = randomCode();
    String base64 = drawCaptcha(code);

    store.put(id, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRE_MS));

    return Map.of("captchaId", id, "captchaImage", "data:image/png;base64," + base64);
  }

  public void verify(String captchaId, String captchaCode) {
    if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
      throw new BusinessException("请输入图片验证码");
    }
    CaptchaEntry entry = store.remove(captchaId);
    if (entry == null) {
      throw new BusinessException("图片验证码已过期，请刷新重试");
    }
    if (System.currentTimeMillis() > entry.expireAt) {
      throw new BusinessException("图片验证码已过期，请刷新重试");
    }
    if (!entry.code.equalsIgnoreCase(captchaCode.trim())) {
      throw new BusinessException("图片验证码错误");
    }
  }

  @Scheduled(fixedRate = 60000)
  public void cleanup() {
    long now = System.currentTimeMillis();
    store.entrySet().removeIf(e -> now > e.getValue().expireAt);
  }

  private String randomCode() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
    }
    return sb.toString();
  }

  private String drawCaptcha(String code) {
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g.setColor(new Color(245, 245, 245));
    g.fillRect(0, 0, WIDTH, HEIGHT);

    for (int i = 0; i < 6; i++) {
      g.setColor(randomLightColor());
      g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
          random.nextInt(WIDTH), random.nextInt(HEIGHT));
    }

    for (int i = 0; i < 30; i++) {
      g.setColor(randomLightColor());
      g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
    }

    Font font = new Font("SansSerif", Font.BOLD, 30);
    int charWidth = WIDTH / (CODE_LENGTH + 1);
    for (int i = 0; i < code.length(); i++) {
      g.setFont(font.deriveFont(AffineTransform.getRotateInstance(
          (random.nextDouble() - 0.5) * 0.4)));
      g.setColor(randomDarkColor());
      g.drawString(String.valueOf(code.charAt(i)),
          charWidth * (i + 1) - 10, 34 + random.nextInt(6) - 3);
    }

    g.dispose();

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return Base64.getEncoder().encodeToString(out.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("验证码图片生成失败", e);
    }
  }

  private Color randomLightColor() {
    return new Color(180 + random.nextInt(60), 180 + random.nextInt(60), 180 + random.nextInt(60));
  }

  private Color randomDarkColor() {
    return new Color(random.nextInt(100), random.nextInt(80), random.nextInt(120));
  }

  private record CaptchaEntry(String code, long expireAt) {}
}
