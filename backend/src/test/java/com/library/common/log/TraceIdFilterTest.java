package com.library.common.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.library.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * traceId 链路测试：不启动 Spring 容器，只装一个探针 Controller 和 TraceIdFilter。
 *
 * <p>覆盖的是「线上排障真正依赖的四个约定」：
 * 每个请求都有 traceId、响应头能拿到它、上游传值优先但必须校验、
 * 500 时错误文案里带追踪号；外加一条负向约定——请求结束后 MDC 必须清空。
 */
@DisplayName("traceId 过滤器")
class TraceIdFilterTest {

    private MockMvc mockMvc;
    private ListAppender<ILoggingEvent> appender;

    @RestController
    static class ProbeController {

        @GetMapping("/api/probe")
        String probe() {
            return "ok";
        }

        @GetMapping("/api/boom")
        String boom() {
            throw new IllegalStateException("模拟数据库连接中断");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .addFilters(new TraceIdFilter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // 挂一个内存 appender（挂在 root 上，覆盖过滤器与 GlobalExceptionHandler 两类日志），
        // 直接断言「日志事件里真的带上了 MDC」，而不是只看响应头
        appender = new ListAppender<>();
        appender.start();
        rootLogger().addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        rootLogger().detachAppender(appender);
        TraceContext.clear();
    }

    private Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    @Test
    @DisplayName("未传 traceId：自动生成 32 位，并通过响应头回传")
    void generateWhenAbsent() throws Exception {
        mockMvc.perform(get("/api/probe"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceContext.HEADER, matchesPattern("[0-9a-f]{32}")));
    }

    @Test
    @DisplayName("上游传入合法 traceId：原样沿用，保证网关/前端能串起同一条链路")
    void reuseUpstreamTraceId() throws Exception {
        mockMvc.perform(get("/api/probe").header(TraceContext.HEADER, "gateway-7f3a9c2b"))
                .andExpect(header().string(TraceContext.HEADER, "gateway-7f3a9c2b"));
    }

    @Test
    @DisplayName("上游传入非法值（空白/换行/过短）时不回显，重新生成，防日志注入")
    void rejectIllegalTraceId() throws Exception {
        for (String illegal : List.of("short", "  ", "abc\ndef injected log line", "x".repeat(65), "<script>")) {
            String returned = mockMvc.perform(get("/api/probe").header(TraceContext.HEADER, illegal))
                    .andReturn().getResponse().getHeader(TraceContext.HEADER);
            assertThat(returned)
                    .as("非法输入 [%s] 必须被替换", illegal)
                    .matches("[0-9a-f]{32}");
        }
    }

    @Test
    @DisplayName("访问日志事件里能读到同一个 traceId，含方法/路径/状态码/耗时字段")
    void accessLogCarriesMdc() throws Exception {
        String traceId = mockMvc.perform(get("/api/probe"))
                .andReturn().getResponse().getHeader(TraceContext.HEADER);

        ILoggingEvent event = appender.list.stream()
                .filter(e -> e.getFormattedMessage().startsWith("GET /api/probe"))
                .findFirst().orElseThrow();

        assertThat(event.getMDCPropertyMap())
                .containsEntry("traceId", traceId)
                .containsEntry("httpStatus", "200")
                .containsEntry("uri", "/api/probe");
        assertThat(event.getMDCPropertyMap().get("costMs")).isNotNull();
    }

    @Test
    @DisplayName("请求结束后 MDC 清空：Tomcat 线程复用不会把 traceId 串到下一个请求")
    void clearMdcAfterRequest() throws Exception {
        mockMvc.perform(get("/api/probe")).andExpect(status().isOk());
        assertThat(TraceContext.get()).isNull();
    }

    @Test
    @DisplayName("500 时错误文案带追踪号，用户截图即可定位")
    void serverErrorExposesTraceId() throws Exception {
        var result = mockMvc.perform(get("/api/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        String traceId = result.getResponse().getHeader(TraceContext.HEADER);
        // 响应体不带 charset，MockMvc 默认按 ISO-8859-1 解码，中文会乱码；
        // 这里只校验 ASCII 的 traceId 是否出现在 body 与日志里，不依赖中文断言
        assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains(traceId);
        // 「系统异常」那条 ERROR（带堆栈）也必须能按同一个 traceId 检索到，
        // 否则用户报障给的追踪号就串不起堆栈
        assertThat(appender.list)
                .anyMatch(e -> e.getLevel() == ch.qos.logback.classic.Level.ERROR
                        && traceId.equals(e.getMDCPropertyMap().get("traceId"))
                        && e.getThrowableProxy() != null);
    }

    @Test
    @DisplayName("静态封面与健康检查不打访问日志，避免刷屏")
    void quietPathsNotLogged() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(header().exists(TraceContext.HEADER));
        // 只看本过滤器产生的日志：Spring MVC 自己也会打「404 没有映射」之类的日志，属正常
        assertThat(appender.list)
                .filteredOn(e -> e.getLoggerName().equals(TraceIdFilter.class.getName()))
                .noneMatch(e -> e.getFormattedMessage().contains("/actuator/health"));
    }
}
