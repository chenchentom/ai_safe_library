package com.aisafe.system.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    @TableId
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private String cssClass;
    private String isDefault;
    private Integer orderNum;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public String getDictLabel() { return dictLabel; }
    public void setDictLabel(String dictLabel) { this.dictLabel = dictLabel; }
    public String getDictValue() { return dictValue; }
    public void setDictValue(String dictValue) { this.dictValue = dictValue; }
    public String getCssClass() { return cssClass; }
    public void setCssClass(String cssClass) { this.cssClass = cssClass; }
    public String getIsDefault() { return isDefault; }
    public void setIsDefault(String isDefault) { this.isDefault = isDefault; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
