package com.library.module.user.init;

import com.library.common.constant.RoleConstants;
import com.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 首次启动初始化默认账号（仅当用户表为空时执行）：
 * admin/admin123（管理员）、reader/123456、reader2/123456（读者）。
 * <p>复用 {@link UserService#createUser} 而不是直接 insert，保证加密方式与唯一校验和正常注册完全一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataInitializer implements ApplicationRunner {

    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (userService.count() > 0) {
            return;
        }
        userService.createUser("admin", "admin123", "系统管理员", RoleConstants.ADMIN);
        userService.createUser("reader", RoleConstants.DEFAULT_PASSWORD, "读者小王", RoleConstants.READER);
        userService.createUser("reader2", RoleConstants.DEFAULT_PASSWORD, "读者小李", RoleConstants.READER);
        log.info("默认账号初始化完成：admin/admin123、reader/123456、reader2/123456");
    }
}
