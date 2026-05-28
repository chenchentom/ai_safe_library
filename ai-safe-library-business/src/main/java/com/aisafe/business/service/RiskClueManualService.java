package com.aisafe.business.service;

import com.aisafe.business.dto.RiskClueManualCreateDTO;

/**
 * 手动新增风险线索 / 安全事件
 */
public interface RiskClueManualService {

    /**
     * 新增待审核线索（audit_status=10，未入库）
     */
    String createClue(RiskClueManualCreateDTO dto);

    /**
     * 新增并直接入库为安全事件（先创建线索，再自动审核入库）
     */
    String createEvent(RiskClueManualCreateDTO dto);
}
