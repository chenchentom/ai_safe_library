package com.aisafe.common.exception;

/**
 * 业务异常 — 用于在 Service 层抛出可预见的业务错误
 * 由 GlobalExceptionHandler 统一捕获并返回前端
 */
public class BusinessException extends RuntimeException {

    /** 状态码 (对应 R.code) */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
