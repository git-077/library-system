package com.library.module.auth.service;

import com.library.module.auth.dto.ChangePasswordDTO;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.LoginVO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.dto.UserInfoVO;

/**
 * 认证服务：只负责「身份」相关的能力（登录、注册、当前用户、改密），
 * 用户的增删改查归 {@code module.user}，两者职责不重叠。
 */
public interface AuthService {

    /**
     * 登录校验，成功返回 JWT 与基础身份信息。
     *
     * @param ip 客户端 IP，用于失败限流；由 Controller 从请求中解析后传入，
     *           这样 Service 不依赖 Servlet API，单元测试可直接调用
     */
    LoginVO login(LoginDTO dto, String ip);

    /** 读者自助注册（角色固定为读者） */
    void register(RegisterDTO dto);

    /** 获取当前登录用户信息（不含密码） */
    UserInfoVO currentUser();

    /** 已登录用户自助修改密码：必须校验原密码 */
    void changePassword(ChangePasswordDTO dto);
}
