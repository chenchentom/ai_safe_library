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

    /**
     * 根据昵称查询用户
     * @param nickname 昵称
     * @return 用户实体，不存在返回 null
     */
    SysUser getByNickname(String nickname);

    /**
     * 用户名是否已被占用（编辑时可排除指定用户）
     */
    boolean isUsernameTaken(String username, Long excludeUserId);

    /**
     * 昵称是否已被占用（编辑时可排除指定用户）
     */
    boolean isNicknameTaken(String nickname, Long excludeUserId);

}
