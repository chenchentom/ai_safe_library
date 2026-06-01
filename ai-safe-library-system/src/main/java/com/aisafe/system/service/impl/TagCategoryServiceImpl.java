package com.aisafe.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aisafe.system.entity.BizTagCategory;
import com.aisafe.system.mapper.BizTagCategoryMapper;
import com.aisafe.system.service.ITagCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分类标签 Service 实现
 *
 * 核心设计:
 * - tag_path 冗余字段: 新增时自动拼接，支持一次 SQL 查全部子树
 * - 缓存策略: 查询缓存整个 module 的树，增删改时清空缓存
 * - 级联删除: 删除节点时一并逻辑删除所有子孙节点
 */
@Service
public class TagCategoryServiceImpl extends ServiceImpl<BizTagCategoryMapper, BizTagCategory>
        implements ITagCategoryService {

    private static final Logger log = LoggerFactory.getLogger(TagCategoryServiceImpl.class);

    @Override
    @Cacheable(value = "tag:tree", key = "#module")
    public List<BizTagCategory> getTreeByModule(String module) {
        LambdaQueryWrapper<BizTagCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizTagCategory::getModule, module)
               .orderByAsc(BizTagCategory::getTagLevel)
               .orderByAsc(BizTagCategory::getSortOrder);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tag:tree", key = "#entity.module")
    public BizTagCategory add(BizTagCategory entity) {
        if (StrUtil.isBlank(entity.getTagCode())) {
            throw new IllegalArgumentException("标签编码不能为空");
        }
        entity.setTagCode(entity.getTagCode().trim());

        // 计算 tag_level 和 tag_path
        if (entity.getParentId() == null || entity.getParentId() == 0) {
            entity.setTagLevel(0);
            if (StrUtil.isBlank(entity.getModule())) {
                throw new IllegalArgumentException("所属模块不能为空");
            }
        } else {
            BizTagCategory parent = baseMapper.selectById(entity.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父节点不存在: " + entity.getParentId());
            }
            entity.setModule(parent.getModule());
            entity.setTagLevel(parent.getTagLevel() + 1);
        }

        assertTagCodeAvailable(entity.getModule(), entity.getTagCode());
        baseMapper.physicalDeleteSoftDeletedByCode(entity.getModule(), entity.getTagCode());

        // 设置默认值
        if (entity.getSortOrder() == null) entity.setSortOrder(0);
        if (StrUtil.isBlank(entity.getStatus())) entity.setStatus("0");

        baseMapper.insert(entity);

        // 回填 tag_path
        if (entity.getParentId() != null && entity.getParentId() > 0) {
            BizTagCategory parent = baseMapper.selectById(entity.getParentId());
            entity.setTagPath(parent.getTagPath() + "/" + entity.getId());
        } else {
            entity.setTagPath("/" + entity.getId());
        }
        baseMapper.updateById(entity);

        log.info("新增标签: id={}, name={}, module={}", entity.getId(), entity.getTagName(), entity.getModule());
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(value = "tag:tree", key = "#entity.module")
    public BizTagCategory update(BizTagCategory entity) {
        BizTagCategory existing = baseMapper.selectById(entity.getId());
        if (existing == null) {
            throw new IllegalArgumentException("标签不存在: " + entity.getId());
        }

        // 模块不允许修改
        entity.setModule(existing.getModule());
        entity.setTagPath(existing.getTagPath());
        entity.setTagLevel(existing.getTagLevel());

        baseMapper.updateById(entity);

        log.info("更新标签: id={}, name={}", entity.getId(), entity.getTagName());
        return baseMapper.selectById(entity.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "tag:tree", allEntries = true)
    public void delete(Long id) {
        BizTagCategory node = baseMapper.selectById(id);
        if (node == null) {
            throw new IllegalArgumentException("标签不存在: " + id);
        }

        // 级联逻辑删除所有子孙节点 (通过 tag_path 前缀匹配)
        LambdaQueryWrapper<BizTagCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizTagCategory::getModule, node.getModule())
               .likeRight(BizTagCategory::getTagPath, node.getTagPath() + "/");

        List<BizTagCategory> children = baseMapper.selectList(wrapper);
        List<Long> idsToDelete = new ArrayList<>();
        idsToDelete.add(id);
        for (BizTagCategory child : children) {
            idsToDelete.add(child.getId());
        }

        baseMapper.deleteBatchIds(idsToDelete);
        log.info("删除标签: id={}, name={}, 级联删除 {} 个子节点", id, node.getTagName(), children.size());
    }

    @Override
    public List<BizTagCategory> search(String keyword) {
        LambdaQueryWrapper<BizTagCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(BizTagCategory::getTagName, keyword)
               .orderByAsc(BizTagCategory::getTagLevel)
               .orderByAsc(BizTagCategory::getSortOrder);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @CacheEvict(value = "tag:tree", allEntries = true)
    public void updateSortOrder(Long id, Integer newSortOrder) {
        BizTagCategory node = baseMapper.selectById(id);
        if (node == null) {
            throw new IllegalArgumentException("标签不存在: " + id);
        }
        node.setSortOrder(newSortOrder);
        baseMapper.updateById(node);
    }

    private void assertTagCodeAvailable(String module, String tagCode) {
        LambdaQueryWrapper<BizTagCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizTagCategory::getModule, module)
               .eq(BizTagCategory::getTagCode, tagCode);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("标签编码已存在: " + tagCode);
        }
    }

    // ==================== Excel 导入导出 (骨架) ====================

    @Override
    @Transactional
    @CacheEvict(value = "tag:tree", allEntries = true)
    public void importExcel(MultipartFile file) {
        // TODO: 实现 POI Excel 导入
        throw new UnsupportedOperationException("Excel 导入功能开发中");
    }

    @Override
    public void exportExcel(HttpServletResponse response, String module) {
        // TODO: 实现 POI Excel 导出
        throw new UnsupportedOperationException("Excel 导出功能开发中");
    }
}
