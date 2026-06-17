-- 线索报告附件 + 批量上传预览
-- 用法: mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/clue_attachment_tables.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_risk_clue_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `clue_id` varchar(64) NOT NULL COMMENT 'ES线索ID',
  `batch_id` bigint DEFAULT NULL COMMENT '批量上传批次ID',
  `serial_no` int DEFAULT NULL COMMENT 'Excel序号',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `stored_name` varchar(255) NOT NULL COMMENT '存储文件名',
  `storage_path` varchar(512) NOT NULL COMMENT '服务器绝对路径',
  `content_type` varchar(128) DEFAULT NULL COMMENT 'MIME类型',
  `file_size` bigint DEFAULT NULL COMMENT '字节',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传人',
  `upload_time` datetime DEFAULT NULL COMMENT '上传时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_clue_id` (`clue_id`),
  KEY `idx_batch_serial` (`batch_id`, `serial_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险线索报告附件';

CREATE TABLE IF NOT EXISTS `biz_risk_report_upload_preview` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `preview_token` varchar(64) NOT NULL COMMENT '预览令牌',
  `excel_path` varchar(512) NOT NULL COMMENT 'Excel暂存路径',
  `excel_file_name` varchar(255) DEFAULT NULL COMMENT 'Excel原始文件名',
  `zip_path` varchar(512) DEFAULT NULL COMMENT 'ZIP暂存路径',
  `zip_file_name` varchar(255) DEFAULT NULL COMMENT 'ZIP原始文件名',
  `extracted_dir` varchar(512) DEFAULT NULL COMMENT 'ZIP解压目录',
  `match_result_json` mediumtext COMMENT '匹配结果JSON',
  `submit_user_id` bigint DEFAULT NULL,
  `submit_org_name` varchar(128) DEFAULT NULL,
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_preview_token` (`preview_token`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量上传预览会话';

ALTER TABLE `biz_risk_report_upload_preview`
  ADD COLUMN `excel_file_name` varchar(255) DEFAULT NULL COMMENT 'Excel原始文件名' AFTER `excel_path`,
  ADD COLUMN `zip_file_name` varchar(255) DEFAULT NULL COMMENT 'ZIP原始文件名' AFTER `zip_path`;

-- 批次表扩展（若列已存在请忽略报错）
ALTER TABLE `biz_risk_report_upload_batch`
  ADD COLUMN `zip_file_name` varchar(255) DEFAULT NULL COMMENT 'ZIP原始文件名' AFTER `file_path`,
  ADD COLUMN `zip_file_path` varchar(512) DEFAULT NULL COMMENT 'ZIP存档路径' AFTER `zip_file_name`,
  ADD COLUMN `extracted_dir` varchar(512) DEFAULT NULL COMMENT '报告解压目录' AFTER `zip_file_path`,
  ADD COLUMN `report_matched_count` int NOT NULL DEFAULT 0 COMMENT '已匹配序号数' AFTER `fail_count`,
  ADD COLUMN `report_missing_count` int NOT NULL DEFAULT 0 COMMENT '缺报告序号数' AFTER `report_matched_count`,
  ADD COLUMN `report_orphan_count` int NOT NULL DEFAULT 0 COMMENT 'ZIP未匹配文件数' AFTER `report_missing_count`,
  ADD COLUMN `report_match_summary` text COMMENT '匹配摘要JSON' AFTER `report_orphan_count`;

ALTER TABLE `biz_risk_report_upload_detail`
  ADD COLUMN `attachment_status` varchar(20) DEFAULT NULL COMMENT 'none/matched/missing' AFTER `error_message`,
  ADD COLUMN `attachment_names` varchar(1000) DEFAULT NULL COMMENT '已绑定报告文件名' AFTER `attachment_status`;
