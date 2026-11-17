package com.library.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录安全策略（application.yml → library.security.login 可调）。
 * <p>默认值按「小团队内网/校园」场景设定，公网部署建议把 max-fail 降到 3。
 */
@Data
@Component
@ConfigurationProperties(prefix = "library.security.login")
public class LoginSecurityProperties {

    /** 同一账号连续失败多少次后锁定 */
    private int maxFail = 5;

    /** 锁定时长（分钟） */
    private int lockMinutes = 15;

    /** 同一 IP 每分钟允许的登录请求数（防止撞库脚本换账号刷） */
    private int ipMaxPerMinute = 20;
}
