package com.aisafe.framework.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 字段自动填充处理器
 *
 * 在 INSERT/UPDATE 时自动设置 BaseEntity 中的审计字段。
 * 这样就不需要在每个 Service 方法中手动设置 createBy / updateBy 等。
 */
@Component
public class EntityMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 尝试获取当前登录用户名，获取不到则用 "system"
        String username = getCurrentUsername();

        this.strictInsertFill(metaObject, "createBy", String.class, username);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateBy", String.class, username);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "delFlag", String.class, "0");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUsername());
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 从 Sa-Token 会话中获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return "system";
        }
    }

}
