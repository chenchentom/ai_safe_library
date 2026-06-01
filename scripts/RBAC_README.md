# RBAC 角色权限使用说明

## 1. 初始化数据库

在 MySQL `ai_safe_library` 库执行：

```bash
mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/platform_menus_reset.sql
# 若菜单中文乱码，再执行:
mysql -u root -p --default-character-set=utf8mb4 ai_safe_library < scripts/fix_menu_names_utf8.sql
```

将创建 `sys_dept_role`、写入菜单与默认角色，并为 `user_id=1` 绑定平台管理员。

## 2. 权限模型

- **角色** 绑定 **菜单**（`sys_role_menu`）
- **用户** 可额外绑定角色（`sys_user_role`）
- **部门** 可绑定角色（`sys_dept_role`），部门下用户继承
- 用户最终权限 = 用户角色 ∪ 部门角色
- `role_key=admin` 的平台管理员拥有全部菜单

## 3. 管理入口

- 系统管理 → **角色管理**：配置角色与菜单
- **部门管理** → 编辑部门 → 部门角色
- **用户管理** → 编辑用户 → 个人角色

## 4. 前端表现

- 侧边栏按登录用户菜单动态渲染
- 无权限的路由访问将跳转 `/403`
