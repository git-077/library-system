package com.library.common.security;

import com.library.common.log.TraceContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 过滤器：从 Authorization 头解析 Token，
 * 校验通过后将 LoginUser 与角色权限注入 SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parseToken(header.substring(7));
            // 未认证时才注入，避免重复设置
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                LoginUser loginUser = new LoginUser(
                        claims.get("userId", Long.class),
                        claims.getSubject(),
                        claims.get("role", String.class));
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + loginUser.getRole().toUpperCase()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // 把操作人写进 MDC：本次请求后续每一行日志（含 SQL、异常堆栈之外的业务日志）
                // 都会带 userId/username，排障时不必再去审计表里按时间反查「是谁」
                TraceContext.markUser(loginUser.getUserId(), loginUser.getUsername());
            }
        }
        chain.doFilter(request, response);
    }
}
