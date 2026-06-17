package com.fanfan.apexsports.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * <p>
 * 定义系统中所有业务错误码及对应消息，按 HTTP 状态码段分类：400xx 参数相关、401xx 认证相关、
 * 403xx 权限相关、404xx 资源不存在、409xx 冲突相关、500xx 系统错误
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
public enum ErrorCode {

    // 400xx - 参数相关
    PARAM_VALIDATION_FAILED(40001, "参数校验失败"),

    // 401xx - 认证相关
    UNAUTHORIZED(40101, "未授权或凭证无效"),
    USERNAME_OR_PASSWORD_ERROR(40101, "用户名或密码错误"),

    // 403xx - 权限相关
    FORBIDDEN(40301, "权限不足"),

    // 404xx - 资源不存在
    USER_NOT_FOUND(40401, "用户不存在"),
    COLLEGE_NOT_FOUND(40402, "学院不存在"),

    // 409xx - 冲突相关
    USERNAME_EXISTS(40901, "用户名已存在"),
    OLD_PASSWORD_WRONG(40901, "原密码错误"),
    NEW_PASSWORD_SAME_AS_OLD(40901, "新密码不能与旧密码相同"),

    // 409xx - 状态冲突
    ACCOUNT_DISABLED(40902, "账号已被禁用"),
    CANNOT_MODIFY_SELF(40902, "不能操作自己的账号"),
    COLLEGE_CODE_EXISTS(40903, "学院代码已存在"),
    COLLEGE_IN_USE(40904, "该学院下存在关联数据，不可删除"),

    // 500xx - 系统错误
    INTERNAL_ERROR(50000, "系统内部错误");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造错误码枚举
     *
     * @param code    错误码
     * @param message 错误消息
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
