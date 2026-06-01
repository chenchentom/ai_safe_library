package com.aisafe.system.dto;

import java.util.List;

public class UserSaveRequest {

    private Long userId;
    private String userName;
    private String nickName;
    private Long deptId;
    private String phonenumber;
    private String email;
    private String status;
    /** 仅新增时使用，明文由后端 BCrypt 加密 */
    private String password;
    private List<Long> roleIds;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getPhonenumber() { return phonenumber; }
    public void setPhonenumber(String phonenumber) { this.phonenumber = phonenumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
