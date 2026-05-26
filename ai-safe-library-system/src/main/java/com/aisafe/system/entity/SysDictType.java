package com.aisafe.system.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @TableId
    private Long id;
    private String dictName;
    private String dictType;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
