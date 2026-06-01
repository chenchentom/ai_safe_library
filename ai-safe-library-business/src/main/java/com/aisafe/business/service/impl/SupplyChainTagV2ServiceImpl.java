package com.aisafe.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aisafe.business.entity.BizSupplyChainTagV2;
import com.aisafe.business.mapper.BizSupplyChainTagV2Mapper;
import com.aisafe.business.service.ISupplyChainTagV2Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplyChainTagV2ServiceImpl extends ServiceImpl<BizSupplyChainTagV2Mapper, BizSupplyChainTagV2>
        implements ISupplyChainTagV2Service {

    private static final Logger log = LoggerFactory.getLogger(SupplyChainTagV2ServiceImpl.class);

    @Override
    public List<BizSupplyChainTagV2> getTree() {
        LambdaQueryWrapper<BizSupplyChainTagV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BizSupplyChainTagV2::getTagLevel)
               .orderByAsc(BizSupplyChainTagV2::getSortOrder);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public BizSupplyChainTagV2 add(BizSupplyChainTagV2 entity) {
        if (StrUtil.isBlank(entity.getTagCode())) {
            throw new IllegalArgumentException("标签编码不能为空");
        }
        entity.setTagCode(entity.getTagCode().trim());

        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }

        if (entity.getParentId() == 0) {
            entity.setTagLevel(0);
            if (StrUtil.isBlank(entity.getModule())) {
                entity.setModule("supply_chain");
            }
        } else {
            BizSupplyChainTagV2 parent = baseMapper.selectById(entity.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父节点不存在: " + entity.getParentId());
            }
            entity.setModule(parent.getModule());
            entity.setTagLevel(parent.getTagLevel() + 1);
        }

        assertTagCodeAvailable(entity.getModule(), entity.getTagCode());
        baseMapper.physicalDeleteSoftDeletedByCode(entity.getModule(), entity.getTagCode());

        if (entity.getSortOrder() == null) entity.setSortOrder(0);
        if (StrUtil.isBlank(entity.getStatus())) entity.setStatus("0");

        baseMapper.insert(entity);

        if (entity.getParentId() != null && entity.getParentId() > 0) {
            BizSupplyChainTagV2 parent = baseMapper.selectById(entity.getParentId());
            entity.setTagPath(parent.getTagPath() + "/" + entity.getId());
        } else {
            entity.setTagPath("/" + entity.getId());
        }
        baseMapper.updateById(entity);

        log.info("新增供应链标签1.0: id={}, name={}", entity.getId(), entity.getTagName());
        return entity;
    }

    @Override
    @Transactional
    public BizSupplyChainTagV2 update(BizSupplyChainTagV2 entity) {
        BizSupplyChainTagV2 existing = baseMapper.selectById(entity.getId());
        if (existing == null) {
            throw new IllegalArgumentException("标签不存在: " + entity.getId());
        }

        entity.setModule(existing.getModule());
        entity.setTagPath(existing.getTagPath());
        entity.setTagLevel(existing.getTagLevel());

        baseMapper.updateById(entity);

        log.info("更新供应链标签1.0: id={}, name={}", entity.getId(), entity.getTagName());
        return baseMapper.selectById(entity.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BizSupplyChainTagV2 node = baseMapper.selectById(id);
        if (node == null) {
            throw new IllegalArgumentException("标签不存在: " + id);
        }

        LambdaQueryWrapper<BizSupplyChainTagV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(BizSupplyChainTagV2::getTagPath, node.getTagPath() + "/");

        List<BizSupplyChainTagV2> children = baseMapper.selectList(wrapper);
        for (BizSupplyChainTagV2 child : children) {
            baseMapper.deleteById(child.getId());
        }
        baseMapper.deleteById(id);

        log.info("删除供应链标签1.0: id={}, name={}, 级联删除 {} 个子节点", id, node.getTagName(), children.size());
    }

    @Override
    public List<BizSupplyChainTagV2> search(String keyword) {
        LambdaQueryWrapper<BizSupplyChainTagV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(BizSupplyChainTagV2::getTagName, keyword)
               .orderByAsc(BizSupplyChainTagV2::getTagLevel)
               .orderByAsc(BizSupplyChainTagV2::getSortOrder);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void updateSortOrder(Long id, Integer newSortOrder) {
        BizSupplyChainTagV2 node = baseMapper.selectById(id);
        if (node == null) {
            throw new IllegalArgumentException("标签不存在: " + id);
        }
        node.setSortOrder(newSortOrder);
        baseMapper.updateById(node);
    }

    private void assertTagCodeAvailable(String module, String tagCode) {
        LambdaQueryWrapper<BizSupplyChainTagV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizSupplyChainTagV2::getModule, module)
               .eq(BizSupplyChainTagV2::getTagCode, tagCode);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("标签编码已存在: " + tagCode);
        }
    }
}
