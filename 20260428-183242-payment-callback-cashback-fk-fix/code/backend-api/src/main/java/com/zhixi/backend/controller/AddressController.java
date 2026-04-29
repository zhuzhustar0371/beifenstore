package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.dto.SaveUserAddressRequest;
import com.zhixi.backend.model.User;
import com.zhixi.backend.model.UserAddress;
import com.zhixi.backend.service.UserAddressService;
import com.zhixi.backend.service.UserAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
  private final UserAddressService userAddressService;
  private final UserAuthService userAuthService;

  public AddressController(UserAddressService userAddressService, UserAuthService userAuthService) {
    this.userAddressService = userAddressService;
    this.userAuthService = userAuthService;
  }

  @GetMapping
  public ApiResponse<List<UserAddress>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(userAddressService.listByUser(currentUser.getId()));
  }

  @GetMapping("/default")
  public ApiResponse<UserAddress> getDefault(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(userAddressService.getPreferredByUser(currentUser.getId()));
  }

  @GetMapping("/{id}")
  public ApiResponse<UserAddress> getById(
      @PathVariable Long id,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(userAddressService.getByIdForUser(id, currentUser.getId()));
  }

  @PostMapping
  public ApiResponse<UserAddress> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody SaveUserAddressRequest request
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(userAddressService.create(currentUser.getId(), request));
  }

  @PutMapping("/{id}")
  public ApiResponse<UserAddress> update(
      @PathVariable Long id,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody SaveUserAddressRequest request
  ) {
    User currentUser = requireUser(authorization);
    return ApiResponse.ok(userAddressService.update(id, currentUser.getId(), request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<String> delete(
      @PathVariable Long id,
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    User currentUser = requireUser(authorization);
    userAddressService.delete(id, currentUser.getId());
    return ApiResponse.ok("OK");
  }

  private User requireUser(String authorization) {
    return userAuthService.getUserByToken(extractToken(authorization));
  }

  private String extractToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return "";
    }
    String prefix = "Bearer ";
    return authorization.startsWith(prefix) ? authorization.substring(prefix.length()).trim() : authorization.trim();
  }
}
