package com.fanfan.apexsports.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应包装类
 * <p>
 * 封装响应状态码、消息和数据，提供成功和失败的静态工厂方法
 * </p>
 *
 * @param <T> 响应数据类型
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    private Result() {}

    /**
     * 返回无数据的成功响应
     * <p>
     * 状态码为 200，消息为"操作成功"，data 为 null
     * </p>
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 返回带数据的成功响应
     * <p>
     * 状态码为 200，消息为"操作成功"
     * </p>
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 返回自定义消息和数据的成功响应
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 返回错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     响应数据类型
     * @return 错误响应
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
