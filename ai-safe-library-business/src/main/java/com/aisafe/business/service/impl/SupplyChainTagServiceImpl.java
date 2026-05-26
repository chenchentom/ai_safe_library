package com.aisafe.business.service.impl;

import com.aisafe.business.entity.BizSupplyChainTag;
import com.aisafe.business.mapper.BizSupplyChainTagMapper;
import com.aisafe.business.service.ISupplyChainTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplyChainTagServiceImpl extends ServiceImpl<BizSupplyChainTagMapper, BizSupplyChainTag>
        implements ISupplyChainTagService {

    private static final Logger log = LoggerFactory.getLogger(SupplyChainTagServiceImpl.class);

    @Override
    public List<BizSupplyChainTag> getTree() {
        LambdaQueryWrapper<BizSupplyChainTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizSupplyChainTag::getStatus, 1)
               .orderByAsc(BizSupplyChainTag::getLevel)
               .orderByAsc(BizSupplyChainTag::getSortOrder);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public BizSupplyChainTag add(BizSupplyChainTag entity) {
        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }

        if (entity.getParentId() == 0) {
            entity.setLevel(1);
        } else {
            BizSupplyChainTag parent = baseMapper.selectById(entity.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父节点不存在: " + entity.getParentId());
            }
            entity.setLevel(parent.getLevel() + 1);
        }

        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getIsLeaf() == null) {
            entity.setIsLeaf(0);
        }

        baseMapper.insert(entity);

        if (entity.getParentId() > 0) {
            BizSupplyChainTag parent = baseMapper.selectById(entity.getParentId());
            entity.setTagPath(parent.getTagPath() + "/" + entity.getId());
            entity.setAncestorIds(parent.getAncestorIds() + "," + entity.getId());
        } else {
            entity.setTagPath("/" + entity.getId());
            entity.setAncestorIds(String.valueOf(entity.getId()));
        }
        baseMapper.updateById(entity);

        log.info("新增供应链标签: id={}, name={}", entity.getId(), entity.getTagName());
        return entity;
    }

    @Override
    @Transactional
    public BizSupplyChainTag update(BizSupplyChainTag entity) {
        BizSupplyChainTag existing = baseMapper.selectById(entity.getId());
        if (existing == null) {
            throw new IllegalArgumentException("标签不存在: " + entity.getId());
        }

        entity.setTagPath(existing.getTagPath());
        entity.setAncestorIds(existing.getAncestorIds());
        entity.setLevel(existing.getLevel());
        entity.setParentId(existing.getParentId());

        baseMapper.updateById(entity);
        log.info("更新供应链标签: id={}, name={}", entity.getId(), entity.getTagName());
        return baseMapper.selectById(entity.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BizSupplyChainTag node = baseMapper.selectById(id);
        if (node == null) {
            throw new IllegalArgumentException("标签不存在: " + id);
        }

        LambdaQueryWrapper<BizSupplyChainTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(BizSupplyChainTag::getTagPath, node.getTagPath() + "/");

        List<BizSupplyChainTag> children = baseMapper.selectList(wrapper);
        for (BizSupplyChainTag child : children) {
            baseMapper.deleteById(child.getId());
        }
        baseMapper.deleteById(id);

        log.info("删除供应链标签: id={}, name={}, 级联删除 {} 个子节点", id, node.getTagName(), children.size());
    }

    @Override
    public List<BizSupplyChainTag> search(String keyword) {
        LambdaQueryWrapper<BizSupplyChainTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizSupplyChainTag::getStatus, 1)
               .and(w -> w.like(BizSupplyChainTag::getTagName, keyword)
                          .or()
                          .like(BizSupplyChainTag::getIntro, keyword)
                          .or()
                          .like(BizSupplyChainTag::getDeveloper, keyword))
               .orderByAsc(BizSupplyChainTag::getLevel)
               .orderByAsc(BizSupplyChainTag::getSortOrder);
        return baseMapper.selectList(wrapper);
    }
}
