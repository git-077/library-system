package com.library.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.user.entity.User;

/**
 * 用户服务：只负责用户档案的增删改查。
 * <p>登录、注册、当前用户等「身份」能力在 {@code module.auth.AuthService}。
 */
public interface UserService extends IService<User> {

    /**
     * 创建用户（密码在此处加密、用户名唯一性在此处校验）。
     * 读者注册与管理员建号共用同一套规则，避免两处逻辑走偏。
     *
     * @param role 为 null 时默认读者
     */
    void createUser(String username, String rawPassword, String realName, String role);

    /** 分页查询用户（按用户名模糊） */
    IPage<User> pageUsers(long pageNum, long pageSize, String username);

    /** 管理员重置用户密码为 123456 */
    void resetPassword(Long userId);

    /** 直接写入已加密的新密码（自助改密用；加密与校验由 AuthService 负责） */
    void updatePassword(Long userId, String encodedPassword);

    /** 更新用户基础信息（真实姓名 / 角色 / 状态），null 字段表示不修改 */
    void updateBaseInfo(Long id, String realName, String role, Integer status);
}
