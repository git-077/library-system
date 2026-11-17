package com.library.module.auth.controller;

import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.common.util.IpUtil;
import com.library.module.auth.dto.ChangePasswordDTO;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.LoginVO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.dto.UserInfoVO;
import com.library.module.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        // IP 在 Controller 解析后传给 Service，Service 不依赖 Servlet API，便于单测
        return Result.ok(authService.login(dto, IpUtil.getClientIp(request)));
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

    /** 自助修改密码：需登录，且必须提供正确的原密码 */
    @PostMapping("/password")
    @Audit(module = "账户", action = "修改密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.ok();
    }
}
