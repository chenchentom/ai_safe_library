-- 重置为 AI 安全平台菜单（与前端路由一致），并赋予 admin 角色全菜单
-- 用法（必须指定 utf8mb4，避免中文乱码）:
--   mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/platform_menus_reset.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_dept_role` (
  `dept_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`dept_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 逻辑删除旧若依菜单（id < 1000）
UPDATE `sys_menu` SET `del_flag` = '1' WHERE `id` < 1000;

-- 删除可能冲突的旧平台菜单 id（若曾半量导入）
UPDATE `sys_menu` SET `del_flag` = '1' WHERE `id` BETWEEN 1000 AND 1999;

-- 平台菜单（id 段 1000+，避免与历史数据冲突）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `status`, `visible`, `del_flag`, `create_time`)
VALUES
(1001, 0, '首页', 'C', '/dashboard', NULL, 'dashboard:view', 'HomeFilled', 1, '0', '0', '0', NOW()),
(1100, 0, '业务中心', 'M', '', NULL, NULL, 'DataAnalysis', 10, '0', '1', '0', NOW()),
(1101, 0, '风险线索库', 'C', '/business/risk-clue', NULL, 'business:risk-clue:list', 'Warning', 11, '0', '0', '0', NOW()),
(1102, 0, '安全事件库', 'C', '/business/security-event', NULL, 'business:security-event:list', 'Document', 12, '0', '0', '0', NOW()),
(1103, 0, '风险报送', 'C', '/business/risk-report', NULL, 'business:risk-report:list', 'Upload', 13, '0', '0', '0', NOW()),
(1104, 1100, '供应链标签', 'C', '/business/supply-chain-tag', NULL, 'business:supply-chain:list', 'Link', 4, '0', '1', '0', NOW()),
(1200, 0, '分类标签', 'M', '', NULL, NULL, 'CollectionTag', 20, '0', '0', '0', NOW()),
(1201, 1200, '风险线索标签', 'C', '/system/tag/risk-clue', NULL, 'system:tag:risk-clue', 'Warning', 1, '0', '0', '0', NOW()),
(1202, 1200, '恶意Skill标签', 'C', '/system/tag/malicious-skill', NULL, 'system:tag:malicious-skill', 'CircleCloseFilled', 2, '0', '0', '0', NOW()),
(1203, 1200, '供应链标签', 'C', '/system/tag/supply-chain-v2', NULL, 'system:tag:supply-chain', 'Link', 3, '0', '0', '0', NOW()),
(1300, 0, '系统管理', 'M', '', NULL, NULL, 'Setting', 30, '0', '0', '0', NOW()),
(1301, 1300, '用户管理', 'C', '/system/user', NULL, 'system:user:list', 'User', 1, '0', '0', '0', NOW()),
(1302, 1300, '部门管理', 'C', '/system/dept', NULL, 'system:dept:list', 'OfficeBuilding', 2, '0', '0', '0', NOW()),
(1303, 1300, '角色管理', 'C', '/system/role', NULL, 'system:role:list', 'UserFilled', 3, '0', '0', '0', NOW()),
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

-- 平台管理员角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `remark`, `del_flag`, `create_time`)
VALUES (1, '平台管理员', 'admin', 1, '1', '0', '超级管理员，全部菜单与数据', '0', NOW())
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `role_key` = 'admin',
  `data_scope` = '1',
  `status` = '0',
  `del_flag` = '0';

-- 角色 1 绑定全部有效平台菜单
DELETE FROM `sys_role_menu` WHERE `role_id` = 1;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`
WHERE `del_flag` = '0' AND `menu_type` IN ('M', 'C') AND `id` >= 1000;

-- admin 用户绑定平台管理员
DELETE FROM `sys_user_role` WHERE `user_id` = 1;
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- admin 所在部门也可挂平台管理员（部门继承）
DELETE FROM `sys_dept_role` WHERE `dept_id` = 1;
INSERT INTO `sys_dept_role` (`dept_id`, `role_id`) VALUES (1, 1);
