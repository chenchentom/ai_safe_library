-- 风险报送批量上传：批次表 + 明细表
-- 用法: mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/risk_report_upload_tables.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_risk_report_upload_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(512) DEFAULT NULL COMMENT '服务器存档路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '数据行总数',
  `processed_count` int NOT NULL DEFAULT 0 COMMENT '已处理行数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功条数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败条数',
  `status` varchar(20) NOT NULL DEFAULT 'processing' COMMENT 'uploading/processing/success/partial_fail/fail',
  `submit_user_id` bigint DEFAULT NULL COMMENT '上传人ID',
  `submit_user_name` varchar(64) DEFAULT NULL COMMENT '上传人昵称',
  `submit_org_name` varchar(128) DEFAULT NULL COMMENT '报送部门',
  `submit_time` datetime DEFAULT NULL COMMENT '开始上传时间',
  `finish_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '整体异常摘要',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_submit_org_time` (`submit_org_name`, `submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险报送上传批次';

CREATE TABLE IF NOT EXISTS `biz_risk_report_upload_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `serial_no` int DEFAULT NULL COMMENT 'Excel序号',
  `event_name` varchar(500) DEFAULT NULL COMMENT '事件名',
  `status` varchar(20) NOT NULL COMMENT 'success/fail',
  `clue_id` varchar(64) DEFAULT NULL COMMENT '成功时ES文档ID',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_batch_status` (`batch_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险报送上传明细';
