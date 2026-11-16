package com.library.module.auth.service;

import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.LoginVO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.dto.UserInfoVO;

/**
 * 认证服务：只负责「身份」相关的能力（登录、注册、当前用户），
 * 用户的增删改查归 {@code module.user}，两者职责不重叠。
 */
public interface AuthService {

    /** 登录校验，成功返回 JWT 与基础身份信息 */
    LoginVO login(LoginDTO dto);

    /** 读者自助注册（角色固定为读者） */
    void register(RegisterDTO dto);

    /** 获取当前登录用户信息（不含密码） */
    UserInfoVO currentUser();
}
