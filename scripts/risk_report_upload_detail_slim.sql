-- 精简上传明细表：仅保留序号、事件名及必要状态字段
-- 用法: mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/risk_report_upload_detail_slim.sql

SET NAMES utf8mb4;

ALTER TABLE `biz_risk_report_upload_detail`
  ADD COLUMN `serial_no` int DEFAULT NULL COMMENT 'Excel序号' AFTER `batch_id`;

UPDATE `biz_risk_report_upload_detail`
SET `serial_no` = `row_num`
WHERE `serial_no` IS NULL AND `row_num` IS NOT NULL;

ALTER TABLE `biz_risk_report_upload_detail`
  DROP COLUMN `row_num`,
  DROP COLUMN `error_code`,
  DROP COLUMN `raw_data_json`;
