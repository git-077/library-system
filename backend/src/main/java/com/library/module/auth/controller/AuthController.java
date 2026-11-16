package com.library.module.auth.controller;

import com.library.common.result.Result;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.LoginVO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.dto.UserInfoVO;
import com.library.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录 / 注册 / 当前用户信息（登录与注册匿名可访问）。
 * <p>控制器只做参数绑定与结果包装，业务规则全部在 {@link AuthService}。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    /** 获取当前登录用户信息（不含密码字段） */
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.ok(authService.currentUser());
    }
}
