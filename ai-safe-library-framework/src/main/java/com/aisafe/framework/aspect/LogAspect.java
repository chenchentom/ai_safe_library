package com.aisafe.framework.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面 (骨架)
 *
 * 阶段一：仅记录请求耗时，不持久化日志。
 * 阶段二：解析注解 @Log，写入 sys_oper_log 表。
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 环绕通知：记录所有 Controller 方法的执行时间
     */
    @Around("execution(* com.aisafe.system.controller..*.*(..))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = point.proceed();
        long cost = System.currentTimeMillis() - start;

        if (cost > 500) {
            log.warn("慢请求: {} 耗时 {}ms", point.getSignature().toShortString(), cost);
        }

        return result;
    }

}
