package com.aisafe.system.controller;

import com.aisafe.common.enums.BusinessType;
import com.aisafe.common.result.R;
import com.aisafe.system.dto.UserSaveRequest;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.AuditLogService;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.ISysUserService;
import com.aisafe.system.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private static final String DEFAULT_PASSWORD_HASH =
            "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final SysPermissionService permissionService;
    private final AuditLogService auditLogService;

    public SysUserController(ISysUserService userService,
                             ISysDeptService deptService,
                             SysPermissionService permissionService,
                             AuditLogService auditLogService) {
        this.userService = userService;
        this.deptService = deptService;
        this.permissionService = permissionService;
        this.auditLogService = auditLogService;
    }

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser user : page.getRecords()) {
            rows.add(toUserMap(user, formatter));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", page.getTotal());
        return R.ok(result);
    }

    @GetMapping("/checkUnique")
    public R<Map<String, Boolean>> checkUnique(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String userId) {
        Long excludeUserId = parseUserId(userId);
        Map<String, Boolean> result = new HashMap<>();
        if (StringUtils.hasText(userName)) {
            result.put("userNameUnique", !userService.isUsernameTaken(userName.trim(), excludeUserId));
        }
        if (StringUtils.hasText(nickName)) {
            result.put("nickNameUnique", !userService.isNicknameTaken(nickName.trim(), excludeUserId));
        }
        return R.ok(result);
    }

    @GetMapping("/{userId}")
    public R<Map<String, Object>> get(@PathVariable String userId) {
        Long parsedUserId = parseUserId(userId);
        if (parsedUserId == null) {
            return R.fail("用户ID无效");
        }
        SysUser user = userService.getById(parsedUserId);
        if (user == null) {
            return R.fail("用户不存在");
        }
        return R.ok(toUserMap(user, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    @PostMapping
    public R<String> add(@RequestBody UserSaveRequest request) {
        R<String> uniqueError = validateUnique(request, null);
        if (uniqueError != null) {
            return uniqueError;
        }
        SysUser user = fromRequest(request);
        user.setPassword(resolvePasswordHash(request.getPassword()));
        userService.save(user);
        permissionService.assignUserRoles(user.getId(), request.getRoleIds());
        return R.ok("新增成功");
    }

    @PutMapping
    public R<String> update(@RequestBody UserSaveRequest request) {
        Long userId = parseUserId(request.getUserId());
        if (userId == null) {
            return R.fail("用户ID不能为空");
        }
        R<String> uniqueError = validateUnique(request, userId);
        if (uniqueError != null) {
            return uniqueError;
        }
        SysUser user = fromRequest(request);
        user.setId(userId);
        user.setPassword(null);
        userService.updateById(user);
        if (request.getRoleIds() != null) {
            permissionService.assignUserRoles(userId, request.getRoleIds());
        }
        return R.ok("修改成功");
    }

    @DeleteMapping("/{ids}")
    public R<String> delete(@PathVariable String ids) {
        List<Map<String, Object>> deletedUsers = new ArrayList<>();
        for (String id : ids.split(",")) {
            Long userId = Long.parseLong(id.trim());
            SysUser user = userService.getById(userId);
            if (user != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", user.getId());
                item.put("userName", user.getUsername());
                item.put("nickName", user.getNickname());
                deletedUsers.add(item);
            }
            permissionService.assignUserRoles(userId, List.of());
            userService.removeById(userId);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("count", deletedUsers.size());
        snapshot.put("users", deletedUsers);
        auditLogService.recordOperSuccess(
                "删除用户", BusinessType.DELETE, "SysUserController.delete", snapshot);
        return R.ok("删除成功");
    }

    @PutMapping("/resetPwd")
    public R<String> resetPwd(@RequestBody Map<String, Object> params) {
        if (params.get("userId") == null) {
            return R.fail("用户ID不能为空");
        }
        Long userId = Long.valueOf(params.get("userId").toString());
        Object pwdObj = params.get("password");
        if (pwdObj == null || !StringUtils.hasText(pwdObj.toString())) {
            return R.fail("新密码不能为空");
        }
        String plain = pwdObj.toString().trim();
        if (plain.length() < 6 || plain.length() > 32) {
            return R.fail("密码长度为 6-32 位");
        }
        SysUser existing = userService.getById(userId);
        if (existing == null) {
            return R.fail("用户不存在");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(new BCryptPasswordEncoder().encode(plain));
        userService.updateById(user);
        return R.ok("重置成功");
    }

    private String resolvePasswordHash(String plainPassword) {
        if (!StringUtils.hasText(plainPassword)) {
            return DEFAULT_PASSWORD_HASH;
        }
        return new BCryptPasswordEncoder().encode(plainPassword.trim());
    }

    private R<String> validateUnique(UserSaveRequest request, Long excludeUserId) {
        if (!StringUtils.hasText(request.getUserName())) {
            return R.fail("用户名不能为空");
        }
        if (!StringUtils.hasText(request.getNickName())) {
            return R.fail("昵称不能为空");
        }
        String username = request.getUserName().trim();
        String nickname = request.getNickName().trim();

        if (excludeUserId != null) {
            SysUser existing = userService.getById(excludeUserId);
            if (existing != null) {
                boolean usernameChanged = !username.equals(existing.getUsername());
                boolean nicknameChanged = !nickname.equals(existing.getNickname());
                if (!usernameChanged && !nicknameChanged) {
                    return null;
                }
                if (usernameChanged && userService.isUsernameTaken(username, excludeUserId)) {
                    return R.fail("用户名已存在");
                }
                if (nicknameChanged && userService.isNicknameTaken(nickname, excludeUserId)) {
                    return R.fail("昵称已存在");
                }
                return null;
            }
        }

        if (userService.isUsernameTaken(username, excludeUserId)) {
            return R.fail("用户名已存在");
        }
        if (userService.isNicknameTaken(nickname, excludeUserId)) {
            return R.fail("昵称已存在");
        }
        return null;
    }

    private Long parseUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private SysUser fromRequest(UserSaveRequest request) {
        SysUser user = new SysUser();
        user.setUsername(StringUtils.hasText(request.getUserName()) ? request.getUserName().trim() : null);
        user.setNickname(StringUtils.hasText(request.getNickName()) ? request.getNickName().trim() : null);
        user.setDeptId(parseUserId(request.getDeptId()));
        user.setPhone(request.getPhonenumber());
        user.setEmail(request.getEmail());
        user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        return user;
    }

    private Map<String, Object> toUserMap(SysUser user, DateTimeFormatter formatter) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getId() != null ? String.valueOf(user.getId()) : null);
        userMap.put("userName", user.getUsername());
        userMap.put("nickName", user.getNickname());
        userMap.put("phonenumber", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("status", user.getStatus());
        userMap.put("deptId", user.getDeptId() != null ? String.valueOf(user.getDeptId()) : null);
        if (user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                Map<String, Object> deptMap = new HashMap<>();
                deptMap.put("deptId", String.valueOf(dept.getId()));
                deptMap.put("deptName", dept.getDeptName());
                userMap.put("dept", deptMap);
            }
        }
        List<Long> roleIds = permissionService.getRoleIdsByUserId(user.getId());
        List<String> roleIdStrings = new ArrayList<>();
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                roleIdStrings.add(String.valueOf(roleId));
            }
        }
        userMap.put("roleIds", roleIdStrings);
        if (user.getCreateTime() != null) {
            userMap.put("createTime", user.getCreateTime().format(formatter));
        }
        return userMap;
    }
}
