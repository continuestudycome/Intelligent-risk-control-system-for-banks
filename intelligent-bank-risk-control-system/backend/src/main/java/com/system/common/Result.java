package com.system.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果封装类
 * 用于标准化 API 接口的返回格式
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
public class Result<T> {
    /** 响应状态码：1-成功，0-失败 */
    private String code;
    
    /** 响应消息描述 */
    private String message;
    
    /** 响应数据载体 */
    private T data;

    /**
     * 全参构造方法
     * @param code 状态码
     * @param message 响应消息
     * @param data 响应数据
     */
    public Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（包含数据）
     * @param data 响应数据
     * @return 封装好的成功响应对象
     * @param <T> 数据类型
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("1", "success", data);
    }

    /**
     * 成功响应（不包含数据）
     * @return 封装好的成功响应对象
     * @param <T> 数据类型
     */
    public static <T> Result<T> success() {
        return new Result<>("1", "success", null);
    }

    /**
     * 失败响应
     * @param message 错误消息描述
     * @return 封装好的失败响应对象
     * @param <T> 数据类型
     */
    public static <T> Result<T> failure(String message) {
        return new Result<>("0", message, null);
    }
}
