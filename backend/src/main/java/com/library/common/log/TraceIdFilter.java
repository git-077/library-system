package com.library.common.log;

import com.library.common.util.IpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * traceId + 访问日志过滤器：整个过滤器链的最外层。
 *
 * <p>顺序 {@link Ordered#HIGHEST_PRECEDENCE} 是刻意为之：它必须包住 Spring Security 链，
 * 这样「未登录 401」「token 过期」这类根本没进 Controller 的请求也有 traceId；
 * 同时 MDC 在 security 之前写入，{@code JwtAuthenticationFilter} 才能补上 userId。
 *
 * <p>一次请求打一行访问日志（method / uri / 状态码 / 耗时），并把状态码与耗时也放进 MDC，
 * 于是这行日志在 JSON 形态下是可直接聚合查询的结构化字段，
 * 而不是要靠正则去抠的一段文本：
 * <pre>
 * {"@timestamp":"...","level":"INFO","traceId":"3f2a...","userId":"1","username":"admin",
 *  "httpMethod":"POST","uri":"/api/books","clientIp":"127.0.0.1","httpStatus":"200","costMs":"18",
 *  "message":"POST /api/books -> 200 (18ms)"}
 * </pre>
 *
 * <p>刻意不记录的：静态封面 {@code /uploads/**}（一次列表页几十个 img）、
 * 健康检查 {@code /actuator/**}（容器每 15s 探活一次，几分钟就把日志刷满）。
 * 这些请求仍然有 traceId，只是不打访问日志行。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 不予记录访问日志的前缀：噪音大、无排障价值 */
    private static final String[] QUIET_PREFIXES = {"/uploads/", "/actuator/", "/favicon."};

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 优先沿用上游（Nginx / 前端 / 网关）传入的合法 traceId，非法值一律重新生成
        String traceId = TraceContext.begin(request.getHeader(TraceContext.HEADER));
        // 回传响应头：前端可在错误提示里显示，用户报障时截图即等于交出 traceId
        response.setHeader(TraceContext.HEADER, traceId);

        TraceContext.put(TraceContext.HTTP_METHOD, request.getMethod());
        TraceContext.put(TraceContext.URI, request.getRequestURI());
        TraceContext.put(TraceContext.CLIENT_IP, IpUtil.getClientIp(request));

        long start = System.nanoTime();
        Throwable failure = null;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            try {
                logAccess(request, response, costMs, failure);
            } finally {
                // 必须清理：Tomcat 线程池会复用线程，残留的 traceId 会污染下一个请求
                TraceContext.clear();
            }
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response,
                           long costMs, Throwable failure) {
        String uri = request.getRequestURI();
        for (String prefix : QUIET_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return;
            }
        }

        int status = response.getStatus();
        TraceContext.put(TraceContext.HTTP_STATUS, String.valueOf(status));
        TraceContext.put(TraceContext.COST_MS, String.valueOf(costMs));

        // 分级：5xx 走 ERROR（可接告警）、4xx 走 WARN（多为客户端问题）、其余 INFO。
        // 级别本身就是筛选条件，比事后 grep 状态码可靠。
        if (status >= 500) {
            log.error("{} {} -> {} ({}ms)", request.getMethod(), uri, status, costMs);
        } else if (status >= 400) {
            log.warn("{} {} -> {} ({}ms)", request.getMethod(), uri, status, costMs);
        } else {
            log.info("{} {} -> {} ({}ms)", request.getMethod(), uri, status, costMs);
        }

        // 异常穿到过滤器层且状态码仍是 2xx：说明异常没经过 GlobalExceptionHandler（如过滤器自身抛错），
        // 补一条带堆栈的日志，否则这类错误只剩容器默认输出，没有 traceId 可查
        if (failure != null && status < 400) {
            log.warn("请求在过滤器链中异常终止：{} {}", request.getMethod(), uri, failure);
        }
    }
}
