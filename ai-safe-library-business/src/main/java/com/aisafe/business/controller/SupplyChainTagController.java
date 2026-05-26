package com.aisafe.business.controller;

import com.aisafe.business.entity.BizSupplyChainTag;
import com.aisafe.business.service.ISupplyChainTagService;
import com.aisafe.common.result.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/biz/supply-chain-tag")
public class SupplyChainTagController {

    private final ISupplyChainTagService tagService;

    public SupplyChainTagController(ISupplyChainTagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        List<BizSupplyChainTag> list = tagService.getTree();
        List<Map<String, Object>> tree = buildTree(list);
        return R.ok(tree);
    }

    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {
        
        LambdaQueryWrapper<BizSupplyChainTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizSupplyChainTag::getStatus, 1);
        
        if (parentId != null && parentId > 0) {
            BizSupplyChainTag parentTag = tagService.getById(parentId);
            if (parentTag != null && parentTag.getTagPath() != null) {
                wrapper.likeRight(BizSupplyChainTag::getTagPath, parentTag.getTagPath() + "/");
            } else {
                wrapper.eq(BizSupplyChainTag::getParentId, parentId);
            }
        }
        
        if (nodeType != null && !nodeType.isEmpty()) {
            wrapper.eq(BizSupplyChainTag::getNodeType, nodeType);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(BizSupplyChainTag::getTagName, keyword)
                                .or()
                                .like(BizSupplyChainTag::getIntro, keyword));
        }
        
        wrapper.orderByAsc(BizSupplyChainTag::getSortOrder);
        
        Page<BizSupplyChainTag> pageResult = tagService.page(new Page<>(page, size), wrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("rows", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public R<BizSupplyChainTag> getById(@PathVariable Long id) {
        BizSupplyChainTag entity = tagService.getById(id);
        if (entity == null) {
            return R.fail("标签不存在");
        }
        return R.ok(entity);
    }

    @PostMapping
    public R<BizSupplyChainTag> add(@RequestBody BizSupplyChainTag entity) {
        try {
            BizSupplyChainTag result = tagService.add(entity);
            return R.ok("新增成功", result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping
    public R<BizSupplyChainTag> update(@RequestBody BizSupplyChainTag entity) {
        try {
            BizSupplyChainTag result = tagService.update(entity);
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
    public R<List<BizSupplyChainTag>> search(@RequestParam String keyword) {
        List<BizSupplyChainTag> result = tagService.search(keyword);
        return R.ok(result);
    }

    @PostMapping("/import")
    public R<String> importExcel() {
        return R.ok("导入功能开发中");
    }

    @GetMapping("/export")
    public R<String> exportExcel() {
        return R.ok("导出功能开发中");
    }

    private List<Map<String, Object>> buildTree(List<BizSupplyChainTag> list) {
        List<Map<String, Object>> tree = new ArrayList<>();
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();

        for (BizSupplyChainTag entity : list) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", entity.getId());
            node.put("tagName", entity.getTagName());
            node.put("parentId", entity.getParentId());
            node.put("nodeType", entity.getNodeType());
            node.put("bizType", entity.getBizType());
            node.put("developer", entity.getDeveloper());
            node.put("intro", entity.getIntro());
            node.put("remark", entity.getRemark());
            node.put("level", entity.getLevel());
            node.put("isLeaf", entity.getIsLeaf());
            node.put("tagPath", entity.getTagPath());
            node.put("ancestorIds", entity.getAncestorIds());
            node.put("sortOrder", entity.getSortOrder());
            node.put("status", entity.getStatus());
            node.put("children", new ArrayList<>());
            nodeMap.put(entity.getId(), node);
        }

        for (BizSupplyChainTag entity : list) {
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
