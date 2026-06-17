package com.aisafe.business.controller;

import com.aisafe.business.entity.BizRiskClueAttachment;
import com.aisafe.business.service.ClueAttachmentService;
import com.aisafe.business.support.ReportDeptSupport;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.common.result.R;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysUserService;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/business/clue-attachment")
public class ClueAttachmentController {

    private final ClueAttachmentService attachmentService;
    private final ISysUserService userService;
    private final ReportDeptSupport reportDeptSupport;

    public ClueAttachmentController(ClueAttachmentService attachmentService,
                                    ISysUserService userService,
                                    ReportDeptSupport reportDeptSupport) {
        this.attachmentService = attachmentService;
        this.userService = userService;
        this.reportDeptSupport = reportDeptSupport;
    }

    @GetMapping("/clue/{clueId}")
    public R<List<Map<String, Object>>> listByClue(@PathVariable String clueId) {
        List<Map<String, Object>> rows = attachmentService.listByClueId(clueId).stream()
                .map(this::toView)
                .collect(Collectors.toList());
        return R.ok(rows);
    }

    @PostMapping("/clue/{clueId}/upload")
    public R<List<Map<String, Object>>> upload(@PathVariable String clueId,
                                              @RequestParam("files") MultipartFile[] files) {
        SysUser user = requireCurrentUser();
        var snapshots = attachmentService.upload(clueId, files, user, resolveDeptName(user));
        List<Map<String, Object>> result = snapshots.stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("fileName", s.getFileName());
            item.put("contentType", s.getContentType());
            item.put("size", s.getSize());
            return item;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        return buildFileResponse(id, true);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return buildFileResponse(id, false);
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        SysUser user = requireCurrentUser();
        String reportUnit = reportDeptSupport.resolveDeptName(user);
        attachmentService.delete(id, user, reportUnit);
        return R.ok("删除成功");
    }

    private ResponseEntity<Resource> buildFileResponse(Long id, boolean inline) {
        BizRiskClueAttachment attachment = attachmentService.requireReadable(id);
        Path path = Path.of(attachment.getStoragePath());
        Resource resource = new FileSystemResource(path);
        String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encodedName;
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(attachment.getContentType());
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    private Map<String, Object> toView(BizRiskClueAttachment row) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(row.getId()));
        map.put("fileName", row.getOriginalName());
        map.put("contentType", row.getContentType());
        map.put("size", row.getFileSize());
        map.put("uploadTime", row.getUploadTime());
        return map;
    }

    private SysUser requireCurrentUser() {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private String resolveDeptName(SysUser user) {
        return reportDeptSupport.resolveDeptName(user);
    }
}
