package com.aisafe.system.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    @TableId
    private Long id;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNoticeTitle() { return noticeTitle; }
    public void setNoticeTitle(String noticeTitle) { this.noticeTitle = noticeTitle; }
    public String getNoticeType() { return noticeType; }
    public void setNoticeType(String noticeType) { this.noticeType = noticeType; }
    public String getNoticeContent() { return noticeContent; }
    public void setNoticeContent(String noticeContent) { this.noticeContent = noticeContent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
