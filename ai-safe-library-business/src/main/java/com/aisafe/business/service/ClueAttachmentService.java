package com.aisafe.business.service;

import com.aisafe.business.dto.ReportAttachmentSnapshot;
import com.aisafe.business.entity.BizRiskClueAttachment;
import com.aisafe.system.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ClueAttachmentService {

    List<BizRiskClueAttachment> listByClueId(String clueId);

    List<ReportAttachmentSnapshot> upload(String clueId, MultipartFile[] files, SysUser user, String deptName);

    void bindBatchFiles(String clueId, Long batchId, Integer serialNo, List<Path> sourceFiles,
                        SysUser user, String deptName);

    BizRiskClueAttachment requireReadable(Long attachmentId);

    void delete(Long attachmentId, SysUser user, String reportUnit);

    void syncEsSnapshot(String clueId);

    /** 删除线索下全部附件（MySQL 记录 + 磁盘文件 + ES 快照） */
    void deleteAllByClueId(String clueId);

    Map<Integer, List<Path>> indexExtractedReports(Path extractedDir);
}
