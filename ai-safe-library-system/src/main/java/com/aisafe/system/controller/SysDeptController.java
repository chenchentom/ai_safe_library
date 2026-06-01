package com.aisafe.system.controller;

import com.aisafe.common.result.R;
import com.aisafe.system.dto.DeptSaveRequest;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门管理控制器
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private final ISysDeptService deptService;
    private final SysPermissionService permissionService;

    public SysDeptController(ISysDeptService deptService, SysPermissionService permissionService) {
        this.deptService = deptService;
        this.permissionService = permissionService;
    }

    /**
     * 获取部门列表（树形结构）
     * GET /api/system/dept/list
     */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysDept::getOrderNum);
        List<SysDept> deptList = deptService.list(wrapper);

        List<Map<String, Object>> result = buildDeptTree(deptList);
        return R.ok(result);
    }

    /**
     * 构建部门树
     */
    private List<Map<String, Object>> buildDeptTree(List<SysDept> deptList) {
        List<Map<String, Object>> tree = new ArrayList<>();
        Map<Long, Map<String, Object>> deptMap = new HashMap<>();

        for (SysDept dept : deptList) {
            Map<String, Object> deptNode = new HashMap<>();
            deptNode.put("deptId", dept.getId());
            deptNode.put("deptName", dept.getDeptName());
            deptNode.put("parentId", dept.getParentId());
            deptNode.put("orderNum", dept.getOrderNum());
            deptNode.put("leader", dept.getLeader());
            deptNode.put("phone", dept.getPhone());
            deptNode.put("email", dept.getEmail());
            deptNode.put("status", dept.getStatus());
            deptNode.put("roleIds", permissionService.getRoleIdsByDeptId(dept.getId()));
            if (dept.getCreateTime() != null) {
                deptNode.put("createTime", dept.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            deptNode.put("children", new ArrayList<>());
            deptMap.put(dept.getId(), deptNode);
        }

        for (SysDept dept : deptList) {
            Map<String, Object> deptNode = deptMap.get(dept.getId());
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                tree.add(deptNode);
            } else {
                Map<String, Object> parentNode = deptMap.get(dept.getParentId());
                if (parentNode != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parentNode.get("children");
                    children.add(deptNode);
                }
            }
        }

        return tree;
    }

    /**
     * 新增部门
     * POST /api/system/dept
     */
    @PostMapping
    public R<Map<String, Object>> add(@RequestBody DeptSaveRequest request) {
        SysDept dept = fromRequest(request);
        deptService.save(dept);
        if (request.getRoleIds() != null) {
            permissionService.assignDeptRoles(dept.getId(), request.getRoleIds());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("deptId", dept.getId());
        return R.ok("新增成功", data);
    }

    @PutMapping
    public R<String> update(@RequestBody DeptSaveRequest request) {
        if (request.getDeptId() == null) {
            return R.fail("部门ID不能为空");
        }
        SysDept dept = fromRequest(request);
        dept.setId(request.getDeptId());
        deptService.updateById(dept);
        if (request.getRoleIds() != null) {
            permissionService.assignDeptRoles(request.getDeptId(), request.getRoleIds());
        }
        return R.ok("修改成功");
    }

    private SysDept fromRequest(DeptSaveRequest request) {
        SysDept dept = new SysDept();
        dept.setParentId(request.getParentId());
        dept.setDeptName(request.getDeptName());
        dept.setOrderNum(request.getOrderNum() != null ? request.getOrderNum() : 0);
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setEmail(request.getEmail());
        dept.setStatus(org.springframework.util.StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        return dept;
    }

    /**
     * 删除部门
     * DELETE /api/system/dept/{id}
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        deptService.removeById(id);
        return R.ok("删除成功");
    }

    @GetMapping("/{deptId}/roleIds")
    public R<List<Long>> getRoleIds(@PathVariable Long deptId) {
        return R.ok(permissionService.getRoleIdsByDeptId(deptId));
    }

    @PutMapping("/{deptId}/roles")
    public R<String> assignRoles(@PathVariable Long deptId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) body.get("roleIds");
        List<Long> roleIds = raw == null ? List.of() : raw.stream().map(Number::longValue).toList();
        permissionService.assignDeptRoles(deptId, roleIds);
        return R.ok("分配成功");
    }
}
