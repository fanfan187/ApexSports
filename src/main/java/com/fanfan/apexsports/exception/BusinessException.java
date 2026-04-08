package com.fanfan.apexsports.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 封装错误码和错误消息，用于在业务逻辑中抛出可预期的异常，由全局异常处理器统一捕获返回
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 使用错误码构造业务异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码和自定义消息构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
