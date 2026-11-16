package com.library.common.exception;

import lombok.Getter;

/**
 * 业务异常：Service 层校验失败时抛出，
 * 由全局异常处理器统一转换为 Result 响应并触发事务回滚
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
