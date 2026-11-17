package com.library.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.common.constant.RoleConstants;
import com.library.common.exception.BizException;
import com.library.module.user.entity.User;
import com.library.module.user.mapper.UserMapper;
import com.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void createUser(String username, String rawPassword, String realName, String role) {
        if (username == null || username.isBlank()) {
            throw new BizException("用户名不能为空");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BizException("密码不能为空");
        }
        String name = username.trim();
        // 用户名唯一校验（含已软删除用户的唯一索引约束，数据库层兜底）
        if (lambdaQuery().eq(User::getUsername, name).count() > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(name);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setRole(role == null || role.isBlank() ? RoleConstants.READER : role);
        user.setStatus(1);
        user.setDeleted(0);
        save(user);
    }

    @Override
    public IPage<User> pageUsers(long pageNum, long pageSize, String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(username != null && !username.isBlank(), User::getUsername, username)
                .orderByDesc(User::getId);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void resetPassword(Long userId) {
        if (getById(userId) == null) {
            throw new BizException("用户不存在");
        }
        update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(RoleConstants.DEFAULT_PASSWORD)));
    }

    @Override
    public void updatePassword(Long userId, String encodedPassword) {
        if (userId == null || encodedPassword == null || encodedPassword.isBlank()) {
            throw new BizException("密码参数不合法");
        }
        boolean updated = update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, encodedPassword));
        if (!updated) {
            throw new BizException("用户不存在");
        }
    }

    @Override
    public void updateBaseInfo(Long id, String realName, String role, Integer status) {
        if (getById(id) == null) {
            throw new BizException("用户不存在");
        }
        update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(realName != null, User::getRealName, realName)
                .set(role != null, User::getRole, role)
                .set(status != null, User::getStatus, status));
    }
}
