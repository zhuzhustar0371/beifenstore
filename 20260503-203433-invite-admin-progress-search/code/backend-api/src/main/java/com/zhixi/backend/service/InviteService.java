package com.zhixi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.InviteProductProgressItem;
import com.zhixi.backend.dto.MiniappInviteProgressVO;
import com.zhixi.backend.dto.MiniappInviteRecordVO;
import com.zhixi.backend.mapper.InviteProductRelationMapper;
import com.zhixi.backend.mapper.InviteRelationMapper;
import com.zhixi.backend.mapper.ProductMapper;
import com.zhixi.backend.mapper.UserMapper;
import com.zhixi.backend.mapper.UserWechatAuthMapper;
import com.zhixi.backend.model.InviteProductRelation;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.Product;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.UserWechatAuth;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InviteService {
  private static final String MINIAPP_INVITE_PAGE = "pages/index/index";
  private static final String WECHAT_SOURCE_MINIAPP = "MINIAPP";
  private static final String WECHAT_SOURCE_WEB = "WEB";

  private final InviteRelationMapper inviteRelationMapper;
  private final InviteProductRelationMapper inviteProductRelationMapper;
  private final ProductMapper productMapper;
  private final UserService userService;
  private final UserMapper userMapper;
  private final UserWechatAuthMapper userWechatAuthMapper;
  private final ObjectMapper objectMapper;
  private final MiniappAccessTokenService miniappAccessTokenService;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public InviteService(
      InviteRelationMapper inviteRelationMapper,
      InviteProductRelationMapper inviteProductRelationMapper,
      ProductMapper productMapper,
      UserService userService,
      UserMapper userMapper,
      UserWechatAuthMapper userWechatAuthMapper,
      ObjectMapper objectMapper,
      MiniappAccessTokenService miniappAccessTokenService
  ) {
    this.inviteRelationMapper = inviteRelationMapper;
    this.inviteProductRelationMapper = inviteProductRelationMapper;
    this.productMapper = productMapper;
    this.userService = userService;
    this.userMapper = userMapper;
    this.userWechatAuthMapper = userWechatAuthMapper;
    this.objectMapper = objectMapper;
    this.miniappAccessTokenService = miniappAccessTokenService;
  }

  public List<InviteRelation> listByInviter(Long inviterId) {
    userService.getUser(inviterId);
    return inviteRelationMapper.findByInviterId(inviterId);
  }

  public List<MiniappInviteRecordVO> listMiniappRecordsByInviter(Long inviterId) {
    userService.getUser(inviterId);
    return inviteRelationMapper.findByInviterId(inviterId)
        .stream()
        .map(this::toMiniappInviteRecordVO)
        .toList();
  }

  public List<MiniappInviteProgressVO> listMiniappProgressByInviter(Long inviterId) {
    userService.getUser(inviterId);
    List<InviteRelation> relations = inviteRelationMapper.findByInviterId(inviterId);
    List<Product> activeProducts = productMapper.findActive();
    int totalProductCount = activeProducts.size();

    return relations.stream().map(relation -> {
      MiniappInviteProgressVO vo = new MiniappInviteProgressVO();
      vo.setId(relation.getId());
      vo.setInviteeId(relation.getInviteeId());
      vo.setBoundAt(relation.getBoundAt());

      User invitee = relation.getInviteeId() == null ? null : userMapper.findById(relation.getInviteeId());
      vo.setInviteeNickname(resolveDisplayNickname(invitee));
      vo.setInviteeAvatarUrl(resolveWechatAvatarUrl(relation.getInviteeId()));

      List<InviteProductRelation> productRelations =
          inviteProductRelationMapper.findByInviterAndInvitee(inviterId, relation.getInviteeId());
      Map<Long, InviteProductRelation> productRelMap = productRelations.stream()
          .collect(Collectors.toMap(InviteProductRelation::getProductId, r -> r));

      int paidCount = 0;
      List<InviteProductProgressItem> items = new ArrayList<>();
      for (Product p : activeProducts) {
        InviteProductProgressItem item = new InviteProductProgressItem();
        item.setProductId(p.getId());
        item.setProductName(p.getName());
        item.setProductImageUrl(p.getImageUrl());
        InviteProductRelation rel = productRelMap.get(p.getId());
        item.setFirstPaidAt(rel != null ? rel.getFirstPaidAt() : null);
        if (item.getFirstPaidAt() != null) {
          paidCount++;
        }
        items.add(item);
      }
      vo.setProducts(items);
      vo.setFirstPaidCount(paidCount);
      vo.setTotalProductCount(totalProductCount);
      return vo;
    }).toList();
  }

  public byte[] createMiniappInviteCode(Long inviterId) {
    User user = userService.getUser(inviterId);
    if (user.getInviteCode() == null || user.getInviteCode().isBlank()) {
      throw new BusinessException("\u5f53\u524d\u8d26\u53f7\u6682\u65e0\u9080\u8bf7\u7801\uff0c\u65e0\u6cd5\u751f\u6210\u9080\u8bf7\u4e8c\u7ef4\u7801");
    }

    String accessToken = miniappAccessTokenService.getAccessToken();
    Map<String, Object> payload = new HashMap<>();
    payload.put("scene", "i=" + inviterId + "&c=" + user.getInviteCode());
    payload.put("page", MINIAPP_INVITE_PAGE);
    payload.put("check_path", false);
    payload.put("env_version", "release");
    payload.put("width", 430);
    return requestMiniappCode(accessToken, payload);
  }

  private byte[] requestMiniappCode(String accessToken, Map<String, Object> payload) {
    try {
      String body = objectMapper.writeValueAsString(payload);
      String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token="
          + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u63a5\u53e3\u8fd4\u56de\u5f02\u5e38\u72b6\u6001: " + response.statusCode());
      }

      byte[] bodyBytes = response.body();
      if (bodyBytes == null || bodyBytes.length == 0) {
        throw new BusinessException("\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u751f\u6210\u5931\u8d25\uff0c\u63a5\u53e3\u8fd4\u56de\u4e3a\u7a7a");
      }

      if (looksLikeJson(response, bodyBytes)) {
        @SuppressWarnings("unchecked")
        Map<String, Object> error = objectMapper.readValue(bodyBytes, Map.class);
        throw new BusinessException(resolveWechatError(error, "\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u751f\u6210\u5931\u8d25"));
      }

      return bodyBytes;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7801\u751f\u6210\u5931\u8d25");
    }
  }

  private boolean looksLikeJson(HttpResponse<byte[]> response, byte[] bodyBytes) {
    String contentType = response.headers().firstValue("Content-Type").orElse("");
    if (contentType.contains("application/json") || contentType.contains("text/plain")) {
      return true;
    }

    for (byte bodyByte : bodyBytes) {
      char ch = (char) bodyByte;
      if (Character.isWhitespace(ch)) {
        continue;
      }
      return ch == '{';
    }
    return false;
  }

  private String resolveWechatError(Map<String, Object> payload, String fallback) {
    Object errCode = payload.get("errcode");
    Object errMsg = payload.get("errmsg");
    if (errCode != null || errMsg != null) {
      return fallback + ": errcode=" + String.valueOf(errCode) + ", errmsg=" + String.valueOf(errMsg);
    }
    return fallback;
  }

  private MiniappInviteRecordVO toMiniappInviteRecordVO(InviteRelation relation) {
    MiniappInviteRecordVO vo = new MiniappInviteRecordVO();
    vo.setId(relation.getId());
    vo.setInviteeId(relation.getInviteeId());
    vo.setBoundAt(relation.getBoundAt());
    vo.setFirstPaidAt(relation.getFirstPaidAt());

    User invitee = relation.getInviteeId() == null ? null : userMapper.findById(relation.getInviteeId());
    vo.setInviteeNickname(resolveDisplayNickname(invitee));
    vo.setInviteeAvatarUrl(resolveWechatAvatarUrl(relation.getInviteeId()));
    return vo;
  }

  private String resolveDisplayNickname(User user) {
    if (user == null) {
      return "";
    }

    String nickname = trimToEmpty(user.getNickname());
    if (!nickname.isBlank() && !isDefaultWechatNickname(nickname)) {
      return nickname;
    }

    String wechatNickname = resolveWechatNickname(user.getId());
    if (!wechatNickname.isBlank()) {
      return wechatNickname;
    }

    return "\u7528\u6237" + user.getId();
  }

  private String resolveWechatNickname(Long userId) {
    if (userId == null) {
      return "";
    }

    UserWechatAuth miniappAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_MINIAPP);
    String miniappNickname = authNickname(miniappAuth);
    if (!miniappNickname.isBlank()) {
      return miniappNickname;
    }

    UserWechatAuth webAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_WEB);
    String webNickname = authNickname(webAuth);
    if (!webNickname.isBlank()) {
      return webNickname;
    }

    UserWechatAuth legacyAuth = userWechatAuthMapper.findByUserId(userId);
    return authNickname(legacyAuth);
  }

  private String resolveWechatAvatarUrl(Long userId) {
    if (userId == null) {
      return "";
    }

    UserWechatAuth miniappAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_MINIAPP);
    String miniappAvatar = authAvatarUrl(miniappAuth);
    if (!miniappAvatar.isBlank()) {
      return miniappAvatar;
    }

    UserWechatAuth webAuth = userWechatAuthMapper.findByUserIdAndSource(userId, WECHAT_SOURCE_WEB);
    String webAvatar = authAvatarUrl(webAuth);
    if (!webAvatar.isBlank()) {
      return webAvatar;
    }

    UserWechatAuth legacyAuth = userWechatAuthMapper.findByUserId(userId);
    return authAvatarUrl(legacyAuth);
  }

  private String authNickname(UserWechatAuth auth) {
    return auth == null ? "" : trimToEmpty(auth.getNickname());
  }

  private String authAvatarUrl(UserWechatAuth auth) {
    return auth == null ? "" : trimToEmpty(auth.getAvatarUrl());
  }

  private String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean isDefaultWechatNickname(String nickname) {
    String value = trimToEmpty(nickname);
    return value.isBlank() || value.matches("(?i)^\u5fae\u4fe1\u7528\u6237[\\w-]{0,16}$");
  }
}
