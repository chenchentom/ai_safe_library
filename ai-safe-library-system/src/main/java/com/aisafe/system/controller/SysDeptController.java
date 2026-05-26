package com.aisafe.system.controller;

import com.aisafe.common.result.R;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.service.ISysDeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
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

    public SysDeptController(ISysDeptService deptService) {
        this.deptService = deptService;
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
    public R<String> add(@RequestBody SysDept dept) {
        deptService.save(dept);
        return R.ok("新增成功");
    }

    /**
     * 修改部门
     * PUT /api/system/dept
     */
    @PutMapping
    public R<String> update(@RequestBody SysDept dept) {
        deptService.updateById(dept);
        return R.ok("修改成功");
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
}
