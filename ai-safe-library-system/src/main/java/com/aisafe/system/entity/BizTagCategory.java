package com.aisafe.system.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 分类标签实体 — 支持无限层级树结构
 * parent_id + tag_path 冗余设计，子树查询一次 SQL 搞定
 */
@TableName("biz_tag_category")
public class BizTagCategory extends BaseEntity {

    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 父节点ID，0=根节点 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 所属模块：risk_clue / malicious_skill / supply_chain */
    private String module;

    /** 标签名称 */
    private String tagName;

    /** 标签编码（唯一，英文标识） */
    private String tagCode;

    /** 层级深度：0=根, 1=一级, 2=二级, … */
    private Integer tagLevel;

    /** ID路径，如 /1/5/12，用于前缀匹配快速子树查询 */
    private String tagPath;

    /** 标签描述 */
    private String description;

    /** 图标（Element Plus icon name） */
    private String icon;

    /** 排序号（同层级内升序） */
    private Integer sortOrder;

    /** 状态：0=启用, 1=停用 */
    private String status;

    // ========== Getter / Setter ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }

    public Integer getTagLevel() { return tagLevel; }
    public void setTagLevel(Integer tagLevel) { this.tagLevel = tagLevel; }

    public String getTagPath() { return tagPath; }
    public void setTagPath(String tagPath) { this.tagPath = tagPath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
