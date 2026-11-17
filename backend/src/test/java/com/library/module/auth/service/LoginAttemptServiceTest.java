package com.library.module.auth.service;

import com.library.common.config.LoginSecurityProperties;
import com.library.common.exception.BizException;
import com.library.module.auth.service.impl.LoginAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 登录防护纯逻辑测试（不启动容器、不连库，毫秒级）。
 * <p>覆盖三条最关键的规则：失败计数归零时机、锁定后的拒绝、IP 限流窗口。
 */
@DisplayName("登录防护：失败锁定与 IP 限流")
class LoginAttemptServiceTest {

    private LoginSecurityProperties props;
    private LoginAttemptServiceImpl service;

    @BeforeEach
    void setUp() {
        props = new LoginSecurityProperties();
        props.setMaxFail(5);
        props.setLockMinutes(15);
        props.setIpMaxPerMinute(20);
        service = new LoginAttemptServiceImpl(props);
    }

    @Test
    @DisplayName("连续失败 5 次后锁定，第 6 次直接被拒绝且提示可读")
    void shouldLockAfterMaxFail() {
        String ip = "10.0.0.1";
        service.ensureLoginAllowed("alice", ip);

        int remaining = -1;
        for (int i = 0; i < 5; i++) {
            remaining = service.recordFailure("alice", ip);
        }
        assertThat(remaining).isZero();

        BizException locked = catchThrowableOfType(
                () -> service.ensureLoginAllowed("alice", ip), BizException.class);
        assertThat(locked).isNotNull();
        assertThat(locked.getCode()).isEqualTo(429);
        assertThat(locked.getMessage()).contains("锁定");
    }

    @Test
    @DisplayName("失败不足上限时返回剩余次数，且不锁定")
    void shouldReturnRemainingAttempts() {
        String ip = "10.0.0.2";
        assertThat(service.recordFailure("bob", ip)).isEqualTo(4);
        assertThat(service.recordFailure("bob", ip)).isEqualTo(3);
        assertThatCode(() -> service.ensureLoginAllowed("bob", ip)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("登录成功清空计数：之前的失败不影响后续尝试")
    void successShouldResetCounter() {
        String ip = "10.0.0.3";
        for (int i = 0; i < 4; i++) {
            service.recordFailure("carol", ip);
        }
        service.recordSuccess("carol");

        assertThat(service.recordFailure("carol", ip)).isEqualTo(4);
    }

    @Test
    @DisplayName("同一 IP 超过每分钟阈值后返回 429，换个账号也拦得住")
    void shouldLimitByIp() {
        props.setIpMaxPerMinute(3);
        String ip = "10.0.0.4";

        service.ensureLoginAllowed("u1", ip);
        service.ensureLoginAllowed("u2", ip);
        service.ensureLoginAllowed("u3", ip);

        assertThatThrownBy(() -> service.ensureLoginAllowed("u4", ip))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("频繁");
    }

    @Test
    @DisplayName("失败计数按账号累计，与来源 IP 无关（换 IP 也绕不过账号锁定）")
    void failureCounterShouldBePerAccount() {
        service.recordFailure("dave", "10.0.0.5");
        assertThat(service.recordFailure("dave", "10.0.0.6")).isEqualTo(3);
    }
}
