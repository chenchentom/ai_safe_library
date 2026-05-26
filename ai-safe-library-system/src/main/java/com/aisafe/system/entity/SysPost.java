package com.aisafe.system.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_post")
public class SysPost extends BaseEntity {

    @TableId
    private Long id;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public Integer getPostSort() { return postSort; }
    public void setPostSort(Integer postSort) { this.postSort = postSort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
