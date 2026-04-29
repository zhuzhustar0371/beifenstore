package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.dto.SaveUserAddressRequest;
import com.zhixi.backend.mapper.UserAddressMapper;
import com.zhixi.backend.model.UserAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAddressService {
  private final UserAddressMapper userAddressMapper;
  private final UserService userService;

  public UserAddressService(UserAddressMapper userAddressMapper, UserService userService) {
    this.userAddressMapper = userAddressMapper;
    this.userService = userService;
  }

  public List<UserAddress> listByUser(Long userId) {
    userService.getUser(userId);
    return userAddressMapper.findByUserId(userId);
  }

  public UserAddress getByIdForUser(Long id, Long userId) {
    userService.getUser(userId);
    UserAddress address = userAddressMapper.findById(id);
    if (address == null || !userId.equals(address.getUserId())) {
      throw new BusinessException("地址不存在");
    }
    return address;
  }

  public UserAddress getPreferredByUser(Long userId) {
    userService.getUser(userId);
    return userAddressMapper.findPreferredByUserId(userId);
  }

  @Transactional
  public UserAddress create(Long userId, SaveUserAddressRequest request) {
    userService.getUser(userId);

    boolean shouldDefault = Boolean.TRUE.equals(request.getIsDefault()) || userAddressMapper.countByUserId(userId) == 0;
    if (shouldDefault) {
      userAddressMapper.clearDefaultByUserId(userId);
    }

    UserAddress address = toAddress(userId, request);
    address.setIsDefault(shouldDefault);
    userAddressMapper.insert(address);
    return userAddressMapper.findById(address.getId());
  }

  @Transactional
  public UserAddress update(Long id, Long userId, SaveUserAddressRequest request) {
    UserAddress existing = getByIdForUser(id, userId);

    boolean shouldDefault = Boolean.TRUE.equals(request.getIsDefault());
    long addressCount = userAddressMapper.countByUserId(userId);
    if (!shouldDefault && Boolean.TRUE.equals(existing.getIsDefault()) && addressCount <= 1) {
      shouldDefault = true;
    }
    if (shouldDefault) {
      userAddressMapper.clearDefaultByUserIdExclude(userId, id);
    }

    UserAddress address = toAddress(userId, request);
    address.setId(id);
    address.setIsDefault(shouldDefault);

    int updated = userAddressMapper.updateByIdAndUserId(address);
    if (updated <= 0) {
      throw new BusinessException("地址保存失败");
    }
    return userAddressMapper.findById(id);
  }

  @Transactional
  public void delete(Long id, Long userId) {
    UserAddress existing = getByIdForUser(id, userId);
    int deleted = userAddressMapper.deleteByIdAndUserId(id, userId);
    if (deleted <= 0) {
      throw new BusinessException("地址删除失败");
    }

    if (Boolean.TRUE.equals(existing.getIsDefault())) {
      UserAddress preferred = userAddressMapper.findPreferredByUserId(userId);
      if (preferred != null) {
        userAddressMapper.markDefaultByIdAndUserId(preferred.getId(), userId);
      }
    }
  }

  private UserAddress toAddress(Long userId, SaveUserAddressRequest request) {
    UserAddress address = new UserAddress();
    address.setUserId(userId);
    address.setRecipientName(trim(request.getRecipientName()));
    address.setRecipientPhone(trim(request.getRecipientPhone()));
    address.setProvince(trim(request.getProvince()));
    address.setCity(trim(request.getCity()));
    address.setDistrict(trim(request.getDistrict()));
    address.setDetailAddress(trim(request.getDetailAddress()));
    return address;
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
