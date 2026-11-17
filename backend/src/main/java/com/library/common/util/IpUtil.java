package com.library.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取。
 * <p>经过 Nginx / 负载均衡时 {@code getRemoteAddr()} 拿到的是代理地址，
 * 必须优先读转发头；{@code X-Forwarded-For} 可能是 {@code 客户端IP, 代理1, 代理2}，
 * 只取第一个（最左侧为真实客户端）。
 * <p>运维审计日志与登录限流都用这里，避免两处实现口径不一致。
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        for (String header : PROXY_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        String remote = request.getRemoteAddr();
        // 本机访问时 Tomcat 可能给出 IPv6 回环地址，统一成 IPv4 便于阅读与匹配
        return "0:0:0:0:0:0:0:1".equals(remote) ? "127.0.0.1" : remote;
    }
}
