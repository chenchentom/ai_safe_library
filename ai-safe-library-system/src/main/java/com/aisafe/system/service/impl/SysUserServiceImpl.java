package com.aisafe.system.service.impl;

import com.aisafe.system.entity.SysUser;
import com.aisafe.system.mapper.SysUserMapper;
import com.aisafe.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户管理 Service 实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    public SysUser getByNickname(String nickname) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getNickname, nickname));
    }

    @Override
    public boolean isUsernameTaken(String username, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean isNicknameTaken(String nickname, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getNickname, nickname);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        return this.count(wrapper) > 0;
    }

}
