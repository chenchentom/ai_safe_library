package com.aisafe.system.service;

import com.aisafe.system.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户管理 Service 接口
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    SysUser getByUsername(String username);

}
