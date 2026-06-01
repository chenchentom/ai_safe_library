-- 1. 隐藏业务中心下的「供应链标签」
-- 2. 将业务中心下其余三个子菜单提升为一级，并隐藏空的「业务中心」目录
-- 用法: mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/menu_adjust_business_tags.sql

SET NAMES utf8mb4;

-- 隐藏业务中心-供应链标签
UPDATE sys_menu SET visible = '1', del_flag = '0' WHERE id = 1104;

-- 业务中心三个子菜单升为一级
UPDATE sys_menu SET parent_id = 0, order_num = 11 WHERE id = 1101;
UPDATE sys_menu SET parent_id = 0, order_num = 12 WHERE id = 1102;
UPDATE sys_menu SET parent_id = 0, order_num = 13 WHERE id = 1103;

-- 隐藏已空的「业务中心」目录
UPDATE sys_menu SET visible = '1', del_flag = '0' WHERE id = 1100;

-- 恢复「分类标签」为目录结构（若曾被误调）
UPDATE sys_menu SET parent_id = 0, visible = '0', order_num = 20 WHERE id = 1200;
UPDATE sys_menu SET parent_id = 1200, order_num = 1 WHERE id = 1201;
UPDATE sys_menu SET parent_id = 1200, order_num = 2 WHERE id = 1202;
UPDATE sys_menu SET parent_id = 1200, order_num = 3 WHERE id = 1203;
