package com.aisafe.business.service;

import com.aisafe.business.dto.RiskClueManualCreateDTO;

/**
 * 手动新增风险线索 / 安全事件
 */
public interface RiskClueManualService {

    /**
     * 新增本部门待审核报送（强制 submit_org_name 为当前部门）
     */
    String createReport(RiskClueManualCreateDTO dto, String reportUnit);

    /**
     * 新增待审核线索（audit_status=10，未入库）
     */
    String createClue(RiskClueManualCreateDTO dto);

    /**
     * 新增并直接入库为安全事件（先创建线索，再自动审核入库）
     */
    String createEvent(RiskClueManualCreateDTO dto);

    /**
     * 根据报送人昵称（或用户名）解析报送部门名称
     */
    String resolveSubmitOrgName(String submitUserName);

    /**
     * 编辑本部门报送的基础信息（待审核/已审核均可）
     */
    void updatePendingReport(String id, RiskClueManualCreateDTO dto, String reportUnit);

    /**
     * 风险线索库：编辑线索基础信息（待审核/已审核均可，不校验报送部门）
     */
    void updatePendingClue(String id, RiskClueManualCreateDTO dto);
}
