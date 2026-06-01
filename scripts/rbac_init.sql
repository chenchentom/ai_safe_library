-- RBAC 初始化：菜单、默认角色、部门角色表
-- 用法: mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/rbac_init.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_dept_role` (
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`dept_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门与角色关联';

-- 建议使用 platform_menus_reset.sql 做完整菜单重置；此处仅保留角色与关联逻辑
-- 逻辑删除旧若依菜单
UPDATE `sys_menu` SET `del_flag` = '1' WHERE `id` < 1000;

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `status`, `visible`, `del_flag`, `create_time`)
VALUES
(1001, 0, '首页', 'C', '/dashboard', NULL, 'dashboard:view', 'HomeFilled', 1, '0', '0', '0', NOW()),
(1100, 0, '业务中心', 'M', '', NULL, NULL, 'DataAnalysis', 10, '0', '0', '0', NOW()),
(1101, 1100, '风险线索库', 'C', '/business/risk-clue', NULL, 'business:risk-clue:list', 'Warning', 1, '0', '0', '0', NOW()),
(1102, 1100, '安全事件库', 'C', '/business/security-event', NULL, 'business:security-event:list', 'Document', 2, '0', '0', '0', NOW()),
(1103, 1100, '风险报送', 'C', '/business/risk-report', NULL, 'business:risk-report:list', 'Upload', 3, '0', '0', '0', NOW()),
(1104, 1100, '供应链标签', 'C', '/business/supply-chain-tag', NULL, 'business:supply-chain:list', 'Link', 4, '0', '0', '0', NOW()),
(1200, 0, '分类标签', 'M', '', NULL, NULL, 'CollectionTag', 20, '0', '0', '0', NOW()),
(1201, 1200, '风险线索标签', 'C', '/system/tag/risk-clue', NULL, 'system:tag:risk-clue', 'Warning', 1, '0', '0', '0', NOW()),
(1202, 1200, '恶意Skill标签', 'C', '/system/tag/malicious-skill', NULL, 'system:tag:malicious-skill', 'CircleCloseFilled', 2, '0', '0', '0', NOW()),
(1203, 1200, '供应链标签', 'C', '/system/tag/supply-chain-v2', NULL, 'system:tag:supply-chain', 'Link', 3, '0', '0', '0', NOW()),
(1300, 0, '系统管理', 'M', '', NULL, NULL, 'Setting', 30, '0', '0', '0', NOW()),
(1301, 1300, '用户管理', 'C', '/system/user', NULL, 'system:user:list', 'User', 1, '0', '0', '0', NOW()),
(1302, 1300, '部门管理', 'C', '/system/dept', NULL, 'system:dept:list', 'OfficeBuilding', 2, '0', '0', '0', NOW()),
(1303, 1300, '角色管理', 'C', '/system/role', NULL, 'system:role:list', 'UserFilled', 3, '0', '0', '0', NOW())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `path` = VALUES(`path`),
  `perms` = VALUES(`perms`),
  `icon` = VALUES(`icon`),
  `order_num` = VALUES(`order_num`),
  `del_flag` = '0';

-- 角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `remark`, `del_flag`, `create_time`)
VALUES
(1, '平台管理员', 'admin', 1, '1', '0', '拥有全部菜单与数据权限', '0', NOW()),
(2, '业务操作员', 'business', 2, '4', '0', '业务中心菜单', '0', NOW()),
(3, '报送员', 'reporter', 3, '5', '0', '风险报送与首页', '0', NOW())
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `data_scope` = VALUES(`data_scope`);

-- 平台管理员：全部菜单
DELETE FROM `sys_role_menu` WHERE `role_id` = 1;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `del_flag` = '0' AND `menu_type` IN ('M', 'C') AND `id` >= 1000;

-- 业务操作员
DELETE FROM `sys_role_menu` WHERE `role_id` = 2;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1001), (2, 1101), (2, 1102), (2, 1103);

-- 报送员
DELETE FROM `sys_role_menu` WHERE `role_id` = 3;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 1001), (3, 1103);

-- 将 admin 用户（id=1，若存在）绑定平台管理员角色
DELETE FROM `sys_user_role` WHERE `user_id` = 1;
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);
