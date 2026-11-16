package com.library.common.constant;

/**
 * 角色常量。
 * <p>数据库与 JWT 中存放的角色标识是 {@code admin} / {@code user}（小写），
 * Spring Security 的 {@code @PreAuthorize} 中使用的是 {@code ROLE_ADMIN} / {@code ROLE_USER}（大写加前缀）。
 * 两套写法必须通过本类常量转换，避免各处散落字符串字面量。
 */
public final class RoleConstants {

    /** 管理员 */
    public static final String ADMIN = "admin";

    /** 读者 */
    public static final String READER = "user";

    /** 默认账号初始化密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    private RoleConstants() {
    }
}
