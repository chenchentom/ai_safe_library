package com.aisafe.system.controller;

import com.aisafe.common.result.R;
import com.aisafe.system.entity.BizTagCategory;
import com.aisafe.system.service.ITagCategoryService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类标签控制器
 *
 * API 设计:
 *   GET    /system/tag/tree?module=     — 获取树结构
 *   GET    /system/tag/{id}            — 获取单个标签
 *   POST   /system/tag                 — 新增
 *   PUT    /system/tag                 — 更新
 *   DELETE /system/tag/{id}            — 删除（级联删除子节点）
 *   GET    /system/tag/search?keyword= — 搜索
 *   PUT    /system/tag/{id}/sort       — 更新排序
 */
@RestController
@RequestMapping("/system/tag")
public class TagCategoryController {

    private final ITagCategoryService tagService;

    public TagCategoryController(ITagCategoryService tagService) {
        this.tagService = tagService;
    }

    /**
     * 获取树 — 返回 el-tree 兼容格式
     * GET /api/system/tag/tree?module=risk_clue
     */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree(@RequestParam(defaultValue = "risk_clue") String module) {
        List<BizTagCategory> list = tagService.getTreeByModule(module);
        List<Map<String, Object>> tree = buildTree(list);
        return R.ok(tree);
    }

    /**
     * 导出标签 Excel
     * GET /api/system/tag/export?module=risk_clue
     */
    @GetMapping("/export")
    public void export(@RequestParam(defaultValue = "risk_clue") String module, HttpServletResponse response) {
        tagService.exportExcel(response, module);
    }

    /**
     * 获取单个标签
     * GET /api/system/tag/{id}
     */
    @GetMapping("/{id}")
    public R<BizTagCategory> getById(@PathVariable Long id) {
        BizTagCategory entity = tagService.getById(id);
        if (entity == null) {
            return R.fail("标签不存在");
        }
        return R.ok(entity);
    }

    /**
     * 新增标签
     * POST /api/system/tag
     */
    @PostMapping
    public R<BizTagCategory> add(@RequestBody BizTagCategory entity) {
        try {
            BizTagCategory result = tagService.add(entity);
            return R.ok("新增成功", result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新标签
     * PUT /api/system/tag
     */
    @PutMapping
    public R<BizTagCategory> update(@RequestBody BizTagCategory entity) {
        try {
            BizTagCategory result = tagService.update(entity);
            return R.ok("更新成功", result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除标签 — 级联删除所有子节点
     * DELETE /api/system/tag/{id}
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        try {
            tagService.delete(id);
            return R.ok("删除成功");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 搜索标签
     * GET /api/system/tag/search?keyword=xxx
     */
    @GetMapping("/search")
    public R<List<BizTagCategory>> search(@RequestParam String keyword) {
        List<BizTagCategory> result = tagService.search(keyword);
        return R.ok(result);
    }

    /**
     * 更新排序
     * PUT /api/system/tag/{id}/sort
     */
    @PutMapping("/{id}/sort")
    public R<String> updateSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        try {
            tagService.updateSortOrder(id, sortOrder);
            return R.ok("排序更新成功");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    // ==================== 树构建工具 ====================

    /**
     * 将平铺列表构建为 el-tree 兼容的树结构
     */
    private List<Map<String, Object>> buildTree(List<BizTagCategory> list) {
        List<Map<String, Object>> tree = new ArrayList<>();
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();

        // 第一遍：创建所有节点
        for (BizTagCategory entity : list) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", String.valueOf(entity.getId()));
            node.put("label", entity.getTagName());
            node.put("parentId", entity.getParentId() == null ? "0" : String.valueOf(entity.getParentId()));
            node.put("tagCode", entity.getTagCode());
            node.put("tagLevel", entity.getTagLevel());
            node.put("tagPath", entity.getTagPath());
            node.put("sortOrder", entity.getSortOrder());
            node.put("status", entity.getStatus());
            node.put("description", entity.getDescription());
            node.put("icon", entity.getIcon());
            node.put("children", new ArrayList<>());
            nodeMap.put(entity.getId(), node);
        }

        // 第二遍：挂载父子关系
        for (BizTagCategory entity : list) {
            Map<String, Object> node = nodeMap.get(entity.getId());
            if (entity.getParentId() == null || entity.getParentId() == 0) {
                tree.add(node);
            } else {
                Map<String, Object> parentNode = nodeMap.get(entity.getParentId());
                if (parentNode != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parentNode.get("children");
                    children.add(node);
                } else {
                    // 父节点不在当前结果集中，挂到根节点
                    tree.add(node);
                }
            }
        }

        return tree;
    }
}
