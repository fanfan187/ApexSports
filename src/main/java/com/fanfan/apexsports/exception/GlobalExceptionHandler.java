package com.fanfan.apexsports.exception;

import com.fanfan.apexsports.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 使用 {@code @RestControllerAdvice} 统一捕获并处理业务异常、参数校验异常和系统异常，
 * 返回标准化的 {@link Result} 响应
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * <p>
     * 捕获 {@link BusinessException} 并返回对应的错误码和消息
     * </p>
     *
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     * <p>
     * 捕获 {@code @Valid} 触发的 {@link MethodArgumentNotValidException}，提取所有字段错误信息
     * </p>
     *
     * @param e 参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.error(ErrorCode.PARAM_VALIDATION_FAILED.getCode(), message);
    }

    /**
     * 处理约束违反异常
     * <p>
     * 捕获 {@code @Validated} 在查询参数上触发的 {@link ConstraintViolationException}
     * </p>
     *
     * @param e 约束违反异常
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_VALIDATION_FAILED.getCode(), e.getMessage());
    }

    /**
     * 兜底异常处理
     * <p>
     * 捕获所有未处理的异常，记录错误日志并返回系统内部错误
     * </p>
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
