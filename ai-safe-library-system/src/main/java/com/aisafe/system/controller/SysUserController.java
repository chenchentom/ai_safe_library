package com.aisafe.system.controller;

import com.aisafe.common.result.R;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final ISysUserService userService;

    public SysUserController(ISysUserService userService) {
        this.userService = userService;
    }

    /**
     * 获取用户列表
     * GET /api/system/user/list
     */
    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String phonenumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(userName)) {
            wrapper.like(SysUser::getUsername, userName);
        }
        if (StringUtils.hasText(phonenumber)) {
            wrapper.like(SysUser::getPhone, phonenumber);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUser::getStatus, status);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = userService.page(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> rows = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (SysUser user : page.getRecords()) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", user.getId());
            userMap.put("userName", user.getUsername());
            userMap.put("nickName", user.getNickname());
            userMap.put("phonenumber", user.getPhone());
            userMap.put("email", user.getEmail());
            userMap.put("status", user.getStatus());
            userMap.put("deptId", user.getDeptId());
            userMap.put("dept", null);
            if (user.getCreateTime() != null) {
                userMap.put("createTime", user.getCreateTime().format(formatter));
            }
            rows.add(userMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", page.getTotal());

        return R.ok(result);
    }

    /**
     * 新增用户
     * POST /api/system/user
     */
    @PostMapping
    public R<String> add(@RequestBody SysUser user) {
        userService.save(user);
        return R.ok("新增成功");
    }

    /**
     * 修改用户
     * PUT /api/system/user
     */
    @PutMapping
    public R<String> update(@RequestBody SysUser user) {
        userService.updateById(user);
        return R.ok("修改成功");
    }

    /**
     * 删除用户
     * DELETE /api/system/user/{ids}
     */
    @DeleteMapping("/{ids}")
    public R<String> delete(@PathVariable String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            userService.removeById(Long.parseLong(id));
        }
        return R.ok("删除成功");
    }

    /**
     * 重置密码
     * PUT /api/system/user/resetPwd
     */
    @PutMapping("/resetPwd")
    public R<String> resetPwd(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword("$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2");
        userService.updateById(user);
        return R.ok("重置成功");
    }
}
