package com.aisafe.system.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleKey() { return roleKey; }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
    public Integer getRoleSort() { return roleSort; }
    public void setRoleSort(Integer roleSort) { this.roleSort = roleSort; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
