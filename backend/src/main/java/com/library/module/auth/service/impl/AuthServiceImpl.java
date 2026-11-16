package com.library.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.constant.RoleConstants;
import com.library.common.exception.BizException;
import com.library.common.security.JwtUtil;
import com.library.common.security.LoginUser;
import com.library.common.security.SecurityUtil;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.LoginVO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.dto.UserInfoVO;
import com.library.module.auth.service.AuthService;
import com.library.module.user.entity.User;
import com.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 用户名去空格，避免前端粘贴时带入不可见空白导致「查不到用户」
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String password = dto.getPassword() == null ? "" : dto.getPassword();

        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            // 失败原因只写日志不返回给前端，便于排查「到底是账号不存在还是密码不对」
            log.warn("登录失败 username=[{}] 账号存在={}", username, user != null);
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("该账户已被禁用，请联系管理员");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public void register(RegisterDTO dto) {
        // 注册只允许创建读者账号，管理员账号只能由管理员在后台创建
        userService.createUser(dto.getUsername(), dto.getPassword(), dto.getRealName(), RoleConstants.READER);
    }

    @Override
    public UserInfoVO currentUser() {
        LoginUser loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null) {
            throw new BizException(401, "未登录");
        }
        User user = userService.getById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(401, "登录用户不存在或已被删除");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        return vo;
    }
}
