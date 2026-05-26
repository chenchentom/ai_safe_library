package com.aisafe.framework.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.common.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 分层处理：
 * 1. 业务异常 BusinessException → 返回对应状态码和消息
 * 2. Sa-Token 未登录 NotLoginException → 401
 * 3. Sa-Token 无权限 NotPermissionException → 403
 * 4. 参数校验异常 BindException → 400
 * 5. 未知异常 Exception → 500（记录日志，不暴露详情）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLoginException(NotLoginException e) {
        return R.fail(401, "请先登录");
    }

    /** 无权限 */
    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermissionException(NotPermissionException e) {
        return R.fail(403, "没有访问权限");
    }

    /** 参数校验失败 */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return R.fail(400, msg);
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(500, "系统内部错误，请联系管理员");
    }

}
