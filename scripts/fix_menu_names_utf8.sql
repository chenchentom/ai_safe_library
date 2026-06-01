SET NAMES utf8mb4;

UPDATE sys_menu SET menu_name = '首页' WHERE id = 1001;
UPDATE sys_menu SET menu_name = '业务中心' WHERE id = 1100;
UPDATE sys_menu SET menu_name = '风险线索库' WHERE id = 1101;
UPDATE sys_menu SET menu_name = '安全事件库' WHERE id = 1102;
UPDATE sys_menu SET menu_name = '风险报送' WHERE id = 1103;
UPDATE sys_menu SET menu_name = '供应链标签' WHERE id = 1104;
UPDATE sys_menu SET menu_name = '分类标签' WHERE id = 1200;
UPDATE sys_menu SET menu_name = '风险线索标签' WHERE id = 1201;
UPDATE sys_menu SET menu_name = '恶意Skill标签' WHERE id = 1202;
UPDATE sys_menu SET menu_name = '供应链标签' WHERE id = 1203;
UPDATE sys_menu SET menu_name = '系统管理' WHERE id = 1300;
UPDATE sys_menu SET menu_name = '用户管理' WHERE id = 1301;
UPDATE sys_menu SET menu_name = '部门管理' WHERE id = 1302;
UPDATE sys_menu SET menu_name = '角色管理' WHERE id = 1303;

UPDATE sys_role SET role_name = '平台管理员', remark = '超级管理员，全部菜单与数据' WHERE id = 1;
UPDATE sys_role SET role_name = '业务操作员', remark = '业务中心菜单' WHERE id = 2;
UPDATE sys_role SET role_name = '报送员', remark = '风险报送与首页' WHERE id = 3;
