package com.library.module.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.log.Audit;
import com.library.common.security.LoginUser;
import com.library.common.security.SecurityUtil;
import com.library.module.log.entity.OperationLog;
import com.library.module.log.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 操作日志切面：拦截 {@link Audit} 标注的写接口，记录「谁、在什么时候、对什么、做了什么、结果如何」。
 *
 * <p>设计要点：
 * <ul>
 *   <li>切在 Controller 而非 Service：能拿到完整 HTTP 上下文（URI / IP / 方法），
 *       且此时 Service 的事务已提交，成功与失败判定准确；</li>
 *   <li>先执行原方法、后写日志，日志写入包在 try/catch 中，绝不因为审计失败而让业务 500；</li>
 *   <li>入参做脱敏（password 一律打码）与截断（1000 字符），避免日志表被大字段撑爆。</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 匹配 JSON 中的密码类字段：password / oldPassword / newPassword 等 */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(\"[A-Za-z]*[Pp]assword\"\\s*:\\s*)\"[^\"]*\"");

    private static final int MAX_PARAM_LENGTH = 1000;
    private static final int MAX_ERROR_LENGTH = 400;

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint point, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return point.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            writeLog(point, audit, System.currentTimeMillis() - start, error);
        }
    }

    private void writeLog(ProceedingJoinPoint point, Audit audit, long costMs, Throwable error) {
        try {
            OperationLog entry = new OperationLog();
            entry.setModule(audit.module());
            entry.setAction(audit.action());
            entry.setCostMs((int) costMs);
            entry.setStatus(error == null ? 1 : 0);
            entry.setCreateTime(LocalDateTime.now());
            if (error != null) {
                entry.setErrorMsg(truncate(error.getMessage() == null
                        ? error.getClass().getSimpleName() : error.getMessage(), MAX_ERROR_LENGTH));
            }

            LoginUser user = SecurityUtil.getLoginUser();
            if (user != null) {
                entry.setUserId(user.getUserId());
                entry.setUsername(user.getUsername());
            }

            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setHttpMethod(request.getMethod());
                entry.setUri(truncate(request.getRequestURI(), 190));
                entry.setIp(clientIp(request));
            }

            if (audit.saveParams()) {
                entry.setParams(serializeArgs(point.getArgs()));
            }

            operationLogService.record(entry);
        } catch (Exception e) {
            // 兜底：连日志对象都构造不出来时也不能影响主流程
            log.warn("操作日志记录异常：{}", e.getMessage());
        }
    }

    /** 序列化入参：剔除容器对象，脱敏密码，超长截断 */
    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> kept = Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(a -> !(a instanceof HttpServletRequest))
                .filter(a -> !(a instanceof jakarta.servlet.http.HttpServletResponse))
                .filter(a -> !(a instanceof MultipartFile))
                .toList();
        if (kept.isEmpty()) {
            return null;
        }
        try {
            Object payload = kept.size() == 1 ? kept.get(0) : kept;
            String json = objectMapper.writeValueAsString(payload);
            json = PASSWORD_PATTERN.matcher(json).replaceAll("$1\"***\"");
            return truncate(json, MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return "<参数序列化失败: " + e.getClass().getSimpleName() + ">";
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    /** 取真实客户端 IP：经过 Nginx 反代时 remoteAddr 是本机，需优先读 X-Forwarded-For */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 60);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp, 60);
        }
        return truncate(request.getRemoteAddr(), 60);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
