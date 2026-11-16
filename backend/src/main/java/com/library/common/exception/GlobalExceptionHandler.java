package com.library.common.exception;

import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：所有错误统一走 Result 格式，Controller 层不需要各自 try-catch。
 * <p>
 * 响应体结构始终为 {code, message, data, timestamp}；
 * 同时让 <b>HTTP 状态码与业务 code 对齐</b>（400/403/404/500），
 * 这样浏览器 DevTools、Nginx 日志、网关监控看到的都是真实语义，
 * 而不是「一律 200」——后者会让排障和告警体系失效。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：code 由抛出方决定（默认 400） */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        int code = e.getCode();
        log.warn("业务异常: code={}, message={}", code, e.getMessage());
        return build(code, e.getMessage());
    }

    /** 参数校验失败：取第一个字段错误信息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return build(HttpStatus.BAD_REQUEST.value(), message);
    }

    /** 权限不足（@PreAuthorize 校验失败） */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        return build(HttpStatus.FORBIDDEN.value(), "无权限执行该操作");
    }

    /**
     * 数据库约束冲突：字段超长、唯一索引重复等。
     * <p>这类问题本质是「客户端提交的数据不合规」，应返回 400 而不是 500，
     * 否则前端只能看到「系统繁忙」，运维也拿不到可读原因。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据约束冲突: {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.BAD_REQUEST.value(), "提交的数据不符合要求：字段可能过长或与已有记录冲突");
    }

    /** 兜底异常：不把堆栈细节暴露给前端，只记录日志 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("系统异常", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后重试");
    }

    /**
     * 业务 code 与 HTTP 状态码映射：
     * 4xx/5xx 直接沿用；其余（如自定义 1001）按 400 返回，避免出现非法 HTTP 状态码。
     */
    private ResponseEntity<Result<Void>> build(int code, String message) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Result.fail(code, message));
    }
}
