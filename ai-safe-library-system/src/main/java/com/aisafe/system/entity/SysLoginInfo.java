package com.aisafe.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_login_info")
public class SysLoginInfo {

    @TableId
    private Long id;
    private String userName;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private String status;
    private String msg;
    private LocalDateTime loginTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getIpaddr() { return ipaddr; }
    public void setIpaddr(String ipaddr) { this.ipaddr = ipaddr; }
    public String getLoginLocation() { return loginLocation; }
    public void setLoginLocation(String loginLocation) { this.loginLocation = loginLocation; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}
