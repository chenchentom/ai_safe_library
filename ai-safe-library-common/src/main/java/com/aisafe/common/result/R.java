package com.aisafe.common.result;

import java.io.Serializable;

/**
 * 统一响应体 — 所有 API 返回值都必须用此类封装
 *
 * @param <T> 响应数据类型
 */
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功码 */
    public static final int SUCCESS = 200;
    /** 失败码 */
    public static final int ERROR = 500;
    /** 未授权 */
    public static final int UNAUTHORIZED = 401;
    /** 禁止访问 */
    public static final int FORBIDDEN = 403;

    /** 状态码 */
    private int code;
    /** 提示消息 */
    private String msg;
    /** 响应数据 */
    private T data;

    public R() {}

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ==================== 静态工厂方法 ====================

    /** 成功，无数据 */
    public static <T> R<T> ok() {
        return new R<>(SUCCESS, "操作成功", null);
    }

    /** 成功，携带数据 */
    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, "操作成功", data);
    }

    /** 成功，自定义消息 + 数据 */
    public static <T> R<T> ok(String msg, T data) {
        return new R<>(SUCCESS, msg, data);
    }

    /** 失败，默认消息 */
    public static <T> R<T> fail() {
        return new R<>(ERROR, "操作失败", null);
    }

    /** 失败，自定义消息 */
    public static <T> R<T> fail(String msg) {
        return new R<>(ERROR, msg, null);
    }

    /** 失败，自定义状态码 + 消息 */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    // ==================== Getter / Setter ====================

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

}
