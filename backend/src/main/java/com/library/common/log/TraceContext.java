package com.library.common.log;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * traceId 上下文：一次请求（或一次定时任务）的唯一标识，
 * 该线程后续打出的每一条日志都会自动带上它。
 *
 * <p>落地方式是最朴素的 <b>SLF4J MDC</b>（ThreadLocal Map）：
 * <ul>
 *   <li>入口过滤器 {@link TraceIdFilter} 写入 traceId，请求结束时清空；</li>
 *   <li>{@code logback-spring.xml} 的 pattern 用 {@code %X{traceId}} 输出，
 *       生产环境的 JSON encoder 则把 MDC 里的所有键直接摊平成字段
 *       （traceId / userId / uri / httpStatus / costMs …），
 *       ELK、Loki 这类系统可以按 traceId 一秒聚合出「一次请求的全部日志」；</li>
 *   <li>响应头 {@code X-Trace-Id} 回传同一个值，前端报错提示里带上它，
 *       用户截图即可让后端直接定位到那次调用。</li>
 * </ul>
 *
 * <p>为什么不用 Spring Cloud Sleuth / Micrometer Tracing：
 * 单机单体应用没有跨服务链路，引入一整套埋点只为了一个随机串，收益不抵复杂度；
 * 需要分布式链路时再把 {@link #begin(String)} 换成 OpenTelemetry 的 traceId 即可，接口不变。
 */
public final class TraceContext {

    /** 响应头 / 上游透传头名称，两边必须一致 */
    public static final String HEADER = "X-Trace-Id";

    /* ---- MDC 键名（同时作为日志 JSON 的字段名，改名等于改日志 schema，谨慎） ---- */
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String URI = "uri";
    public static final String CLIENT_IP = "clientIp";
    public static final String HTTP_STATUS = "httpStatus";
    public static final String COST_MS = "costMs";

    /**
     * 合法 traceId：8~64 位字母、数字、下划线、短横线。
     * <p>必须白名单校验——上游头是用户可控输入，直接把 {@code "a\n2026-01-01 ERROR 伪造" }
     * 写进日志就能伪造日志行（日志注入），也会破坏 JSON 结构。
     */
    private static final Pattern VALID_TRACE_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    /** 请求结束时统一清理，避免 Tomcat 线程复用导致 traceId 串到下个请求 */
    private static final String[] ALL_KEYS = {
            TRACE_ID, USER_ID, USERNAME, HTTP_METHOD, URI, CLIENT_IP, HTTP_STATUS, COST_MS
    };

    private TraceContext() {
    }

    /** 生成 32 位无横线 UUID，短、可读、碰撞概率可忽略 */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 当前线程的 traceId；非请求线程（启动期、无过滤器的线程）返回 null */
    public static String get() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 开启一次追踪：优先沿用上游传入的合法 traceId（便于网关/前端串链），否则新生成。
     *
     * @return 实际生效的 traceId
     */
    public static String begin(String candidate) {
        String traceId = isValid(candidate) ? candidate : newTraceId();
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    /** 非 HTTP 入口（定时任务、消息消费）开启追踪 */
    public static String beginNew() {
        return begin(null);
    }

    public static boolean isValid(String candidate) {
        return candidate != null && VALID_TRACE_ID.matcher(candidate).matches();
    }

    /** 鉴权通过后补上操作人，让「谁触发的」和「哪个 traceId」出现在同一行日志里 */
    public static void markUser(Long userId, String username) {
        put(USER_ID, userId == null ? null : String.valueOf(userId));
        put(USERNAME, username);
    }

    /** 写 MDC；值为 null 时删除而不是写 "null" 字符串 */
    public static void put(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    /** 清空本类写入的所有键（只删自己的，不动框架可能写入的内容） */
    public static void clear() {
        for (String key : ALL_KEYS) {
            MDC.remove(key);
        }
    }
}
