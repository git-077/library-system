package com.library.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类：生成 / 解析 Token
 * Token 载荷包含 userId、username、role，过滤器据此还原登录用户
 */
@Component
public class JwtUtil {

    @Value("${library.jwt.secret}")
    private String secret;

    @Value("${library.jwt.expire-hours:2}")
    private long expireHours;

    private Key key() {
        // HS256 要求密钥长度 >= 32 字节
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireHours * 3600_000))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析失败（过期 / 篡改 / 格式错误）统一返回 null */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
