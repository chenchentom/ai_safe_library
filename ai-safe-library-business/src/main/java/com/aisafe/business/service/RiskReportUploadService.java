package com.aisafe.business.service;

import com.aisafe.business.entity.BizRiskReportUploadBatch;
import com.aisafe.business.entity.BizRiskReportUploadDetail;
import com.aisafe.system.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface RiskReportUploadService {

    /**
     * 校验 Excel（及可选 ZIP）匹配情况，不落库
     */
    Map<String, Object> previewUpload(MultipartFile excel, MultipartFile zip, SysUser user, String submitOrgName);

    /**
     * 确认导入（基于 previewToken）
     */
    Map<String, Object> confirmUpload(String previewToken, SysUser user, String submitOrgName);

    /**
     * 保存文件并创建批次，异步处理导入（可选 ZIP 报告包）
     */
    Map<String, Object> startUpload(MultipartFile file, MultipartFile zip, SysUser user, String submitOrgName);

    void processBatchAsync(Long batchId);

    Map<String, Object> getProgress(Long batchId, SysUser user, String submitOrgName, boolean superAdmin);

    Map<String, Object> listBatches(int page, int size, String status, String keyword,
                                    String submitUserName, String submitTimeStart, String submitTimeEnd,
                                    SysUser user, String submitOrgName, boolean superAdmin);

    Map<String, Object> getBatch(Long batchId, SysUser user, String submitOrgName, boolean superAdmin);

    Map<String, Object> listDetails(Long batchId, int page, int size, String status,
                                    SysUser user, String submitOrgName, boolean superAdmin);
}
