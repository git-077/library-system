package com.library.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtUtil 单元测试：不依赖 Spring 容器与数据库。
 * <p>
 * 覆盖 token 生成/解析、载荷正确性、篡改与过期两类失败场景。
 */
@DisplayName("JwtUtil 令牌工具")
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdefghijklmn";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 2L);
    }

    @Test
    @DisplayName("生成的 token 能被解析，且 userId / role 载荷不丢失")
    void generateThenParse_shouldKeepClaims() {
        String token = jwtUtil.generateToken(7L, "reader", "user");

        assertThat(token).isNotBlank();
        Claims claims = jwtUtil.parseToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("reader");
        // JSON 反序列化后数字为 Integer，统一按 long 比较
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(7L);
        assertThat(claims.get("role")).isEqualTo("user");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("篡改过的 token 解析返回 null，不抛异常")
    void parseTamperedToken_shouldReturnNull() {
        String token = jwtUtil.generateToken(1L, "admin", "admin");
        // 改动签名部分的最后一个字符
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtUtil.parseToken(tampered)).isNull();
    }

    @Test
    @DisplayName("格式非法或为空的 token 解析返回 null")
    void parseMalformedToken_shouldReturnNull() {
        assertThat(jwtUtil.parseToken("not-a-jwt")).isNull();
        assertThat(jwtUtil.parseToken("")).isNull();
    }

    @Test
    @DisplayName("已过期的 token 解析返回 null")
    void parseExpiredToken_shouldReturnNull() {
        // 有效期设为负数，生成即过期
        ReflectionTestUtils.setField(jwtUtil, "expireHours", -1L);
        String expired = jwtUtil.generateToken(1L, "admin", "admin");

        assertThat(jwtUtil.parseToken(expired)).isNull();
    }

    @Test
    @DisplayName("换用其他密钥签发的 token 无法通过校验")
    void parseTokenSignedByOtherKey_shouldReturnNull() {
        String token = jwtUtil.generateToken(1L, "admin", "admin");

        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "another-secret-key-0123456789abcdefghijkl");
        ReflectionTestUtils.setField(other, "expireHours", 2L);

        assertThat(other.parseToken(token)).isNull();
    }
}
