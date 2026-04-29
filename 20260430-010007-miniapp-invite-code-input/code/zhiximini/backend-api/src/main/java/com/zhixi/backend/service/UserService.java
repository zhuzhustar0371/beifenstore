package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.common.PasswordCodec;
import com.zhixi.backend.dto.RegisterRequest;
import com.zhixi.backend.mapper.InviteRelationMapper;
import com.zhixi.backend.mapper.UserMapper;
import com.zhixi.backend.model.InviteRelation;
import com.zhixi.backend.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {
  private final UserMapper userMapper;
  private final InviteRelationMapper inviteRelationMapper;

  public UserService(UserMapper userMapper, InviteRelationMapper inviteRelationMapper) {
    this.userMapper = userMapper;
    this.inviteRelationMapper = inviteRelationMapper;
  }

  @Transactional
  public User register(RegisterRequest request) {
    if (userMapper.findByPhone(request.getPhone()) != null) {
      throw new BusinessException("该手机号已注册");
    }

    Long inviterId = null;
    if (request.getInviteCode() != null && !request.getInviteCode().isBlank()) {
      User inviter = userMapper.findByInviteCode(request.getInviteCode());
      if (inviter == null) {
        throw new BusinessException("邀请码无效");
      }
      inviterId = inviter.getId();
    }

    User user = new User();
    user.setPhone(request.getPhone());
    user.setPasswordHash(PasswordCodec.sha256(request.getPassword()));
    String nickname = request.getNickname();
    user.setNickname((nickname == null || nickname.isBlank()) ? "知禧用户" : nickname);
    user.setInviterId(inviterId);
    user.setInviteCode(generateInviteCode());
    userMapper.insert(user);

    if (inviterId != null) {
      InviteRelation relation = new InviteRelation();
      relation.setInviterId(inviterId);
      relation.setInviteeId(user.getId());
      inviteRelationMapper.insert(relation);
    }

    return userMapper.findById(user.getId());
  }

  public User getUser(Long id) {
    User user = userMapper.findById(id);
    if (user == null) {
      throw new BusinessException("用户不存在");
    }
    return user;
  }

  public long totalUsers() {
    return userMapper.countAll();
  }

  public User findByPhone(String phone) {
    return userMapper.findByPhone(phone);
  }

  @Transactional
  public User createFromPhone(String phone, String nickname, String inviteCode) {
    if (userMapper.findByPhone(phone) != null) {
      throw new BusinessException("手机号已存在");
    }

    Long inviterId = null;
    if (inviteCode != null && !inviteCode.isBlank()) {
      User inviter = userMapper.findByInviteCode(inviteCode);
      if (inviter == null) {
        throw new BusinessException("邀请码无效");
      }
      inviterId = inviter.getId();
    }

    User user = new User();
    user.setPhone(phone);
    user.setNickname((nickname == null || nickname.isBlank()) ? "微信用户" : nickname);
    user.setInviterId(inviterId);
    user.setInviteCode(generateInviteCode());
    userMapper.insert(user);

    if (inviterId != null) {
      InviteRelation relation = new InviteRelation();
      relation.setInviterId(inviterId);
      relation.setInviteeId(user.getId());
      inviteRelationMapper.insert(relation);
    }
    return userMapper.findById(user.getId());
  }

  @Transactional(noRollbackFor = BusinessException.class)
  public void bindInviterIfNeeded(Long userId, String inviteCode) {
    if (inviteCode == null || inviteCode.isBlank()) {
      return;
    }
    User user = getUser(userId);
    if (user.getInviterId() != null) {
      return;
    }
    User inviter = userMapper.findByInviteCode(inviteCode);
    if (inviter == null || inviter.getId().equals(userId)) {
      throw new BusinessException("邀请码无效");
    }
    int changed = userMapper.bindInviter(userId, inviter.getId());
    if (changed > 0) {
      InviteRelation relation = new InviteRelation();
      relation.setInviterId(inviter.getId());
      relation.setInviteeId(userId);
      inviteRelationMapper.insert(relation);
    }
  }

  @Transactional
  public void bindInviterByIdIfNeeded(Long userId, Long inviterId) {
    if (userId == null || inviterId == null) {
      return;
    }
    User user = getUser(userId);
    if (user.getInviterId() != null) {
      return;
    }
    if (userId.equals(inviterId)) {
      throw new BusinessException("邀请人不能是自己");
    }
    User inviter = getUser(inviterId);
    int changed = userMapper.bindInviter(userId, inviter.getId());
    if (changed > 0) {
      InviteRelation relation = new InviteRelation();
      relation.setInviterId(inviter.getId());
      relation.setInviteeId(userId);
      inviteRelationMapper.insert(relation);
    }
  }

  public User findByMiniappOpenid(String openid) {
    return userMapper.findByMiniappOpenid(openid);
  }

  @Transactional
  public void updateMiniappOpenid(Long userId, String openid) {
    userMapper.updateMiniappOpenid(userId, openid);
  }

  @Transactional
  public void updateNickname(Long userId, String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return;
    }
    userMapper.updateNickname(userId, nickname.trim());
  }

  @Transactional
  public void resetPasswordByPhone(String phone, String newPassword) {
    User user = userMapper.findByPhone(phone);
    if (user == null) {
      throw new BusinessException("账号不存在，请先注册");
    }
    userMapper.updatePasswordByPhone(phone, PasswordCodec.sha256(newPassword));
  }

  private String generateInviteCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }
}
