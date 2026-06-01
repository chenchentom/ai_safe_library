package com.aisafe.business.service;

import com.aisafe.business.entity.BizRiskReportUploadBatch;
import com.aisafe.business.entity.BizRiskReportUploadDetail;
import com.aisafe.system.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface RiskReportUploadService {

    /**
     * 保存文件并创建批次，异步处理导入
     */
    Map<String, Object> startUpload(MultipartFile file, SysUser user, String submitOrgName);

    void processBatchAsync(Long batchId);

    Map<String, Object> getProgress(Long batchId, SysUser user, String submitOrgName, boolean superAdmin);

    Map<String, Object> listBatches(int page, int size, String status, String keyword,
                                    String submitUserName, String submitTimeStart, String submitTimeEnd,
                                    SysUser user, String submitOrgName, boolean superAdmin);

    Map<String, Object> getBatch(Long batchId, SysUser user, String submitOrgName, boolean superAdmin);

    Map<String, Object> listDetails(Long batchId, int page, int size, String status,
                                    SysUser user, String submitOrgName, boolean superAdmin);
}
