package com.aisafe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_dept_role")
public class SysDeptRole {

    private Long deptId;
    private Long roleId;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
