package com.aisafe.business.controller;

import com.aisafe.business.entity.BizSupplyChainTagV2;
import com.aisafe.business.service.ISupplyChainTagV2Service;
import com.aisafe.common.result.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/biz/supply-chain-tag-v2")
public class SupplyChainTagV2Controller {

    private final ISupplyChainTagV2Service tagService;

    public SupplyChainTagV2Controller(ISupplyChainTagV2Service tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        List<BizSupplyChainTagV2> list = tagService.getTree();
        List<Map<String, Object>> tree = buildTree(list);
        return R.ok(tree);
    }

    @GetMapping("/{id}")
    public R<BizSupplyChainTagV2> getById(@PathVariable Long id) {
        BizSupplyChainTagV2 entity = tagService.getById(id);
        if (entity == null) {
            return R.fail("标签不存在");
        }
        return R.ok(entity);
    }

    @PostMapping
    public R<BizSupplyChainTagV2> add(@RequestBody BizSupplyChainTagV2 entity) {
        try {
            BizSupplyChainTagV2 result = tagService.add(entity);
            return R.ok("新增成功", result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping
    public R<BizSupplyChainTagV2> update(@RequestBody BizSupplyChainTagV2 entity) {
        try {
            BizSupplyChainTagV2 result = tagService.update(entity);
            return R.ok("更新成功", result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        try {
            tagService.delete(id);
            return R.ok("删除成功");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/search")
    public R<List<BizSupplyChainTagV2>> search(@RequestParam String keyword) {
        List<BizSupplyChainTagV2> result = tagService.search(keyword);
        return R.ok(result);
    }

    @PutMapping("/{id}/sort")
    public R<String> updateSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        try {
            tagService.updateSortOrder(id, sortOrder);
            return R.ok("排序更新成功");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 导出供应链标签 Excel
     * GET /api/biz/supply-chain-tag-v2/export
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        tagService.exportExcel(response);
    }

    private List<Map<String, Object>> buildTree(List<BizSupplyChainTagV2> list) {
        List<Map<String, Object>> tree = new ArrayList<>();
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();

        for (BizSupplyChainTagV2 entity : list) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", String.valueOf(entity.getId()));
            node.put("label", entity.getTagName());
            node.put("parentId", entity.getParentId() == null ? "0" : String.valueOf(entity.getParentId()));
            node.put("module", entity.getModule());
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

        for (BizSupplyChainTagV2 entity : list) {
            Map<String, Object> node = nodeMap.get(entity.getId());
            Long parentId = entity.getParentId();
            if (parentId != null && parentId > 0) {
                Map<String, Object> parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    node.put("parentName", parentNode.get("label"));
                } else {
                    node.put("parentName", entity.getParentName());
                }
            } else {
                node.put("parentName", "");
            }
        }

        for (BizSupplyChainTagV2 entity : list) {
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
                    tree.add(node);
                }
            }
        }

        return tree;
    }
}
