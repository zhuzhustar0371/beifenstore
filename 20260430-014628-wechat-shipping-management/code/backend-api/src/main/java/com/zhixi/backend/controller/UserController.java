package com.zhixi.backend.controller;

import com.zhixi.backend.common.ApiResponse;
import com.zhixi.backend.dto.RegisterRequest;
import com.zhixi.backend.model.User;
import com.zhixi.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public ApiResponse<User> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok(userService.register(request));
  }

  @GetMapping("/{id}")
  public ApiResponse<User> get(@PathVariable Long id) {
    return ApiResponse.ok(userService.getUser(id));
  }
}
