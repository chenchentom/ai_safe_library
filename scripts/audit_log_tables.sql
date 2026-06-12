-- 操作日志 & 登录日志表 + 系统管理菜单
-- 用法: mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/audit_log_tables.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(100) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT 0 COMMENT '业务类型 0其它 1新增 2修改 3删除 4审核 5授权 6导入',
  `method` varchar(200) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `oper_name` varchar(64) DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(64) DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` text COMMENT '请求参数摘要',
  `json_result` text COMMENT '返回参数',
  `status` int DEFAULT 0 COMMENT '0正常 1异常',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT 0 COMMENT '消耗时间(毫秒)',
  PRIMARY KEY (`id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_oper_name` (`oper_name`),
  KEY `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

CREATE TABLE IF NOT EXISTS `sys_login_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(64) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(64) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(64) DEFAULT '' COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态 0成功 1失败',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统访问记录';

-- 审计日志菜单（系统管理下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `status`, `visible`, `del_flag`, `create_time`)
VALUES
(1304, 1300, '操作日志', 'C', '/system/oper-log', NULL, 'system:oper-log:list', 'Document', 4, '0', '0', '0', NOW()),
(1305, 1300, '登录日志', 'C', '/system/login-info', NULL, 'system:login-info:list', 'Key', 5, '0', '0', '0', NOW())
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `menu_name` = VALUES(`menu_name`),
  `menu_type` = VALUES(`menu_type`),
  `path` = VALUES(`path`),
  `perms` = VALUES(`perms`),
  `icon` = VALUES(`icon`),
  `order_num` = VALUES(`order_num`),
  `status` = '0',
  `visible` = '0',
  `del_flag` = '0';

-- 平台管理员角色绑定新菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 1304), (1, 1305);
