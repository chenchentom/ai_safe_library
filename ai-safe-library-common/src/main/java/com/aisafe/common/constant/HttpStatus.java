package com.aisafe.common.constant;

/**
 * HTTP 响应状态码常量
 * 业务状态码统一管理，避免魔法数字
 */
public class HttpStatus {

    /** 成功 */
    public static final int SUCCESS = 200;
    /** 参数错误 */
    public static final int BAD_REQUEST = 400;
    /** 未登录 / Token 过期 */
    public static final int UNAUTHORIZED = 401;
    /** 无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 系统内部错误 */
    public static final int ERROR = 500;

}
