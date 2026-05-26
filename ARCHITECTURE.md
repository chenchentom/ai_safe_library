# AI安全情报平台 - 架构设计文档

## 项目概述

AI安全情报平台（ai_safe_library）是一个企业级安全情报管理系统，用于采集、分析、审核和管理各类安全风险线索。

## 技术栈

### 后端

- **框架**: Spring Boot 3.2.5 + Java 17
- **认证**: Sa-Token JWT（token-prefix: Bearer）
- **ORM**: MyBatis Plus
- **搜索引擎**: Elasticsearch 8.14.3
- **缓存**: Redis (Spring Cache)
- **构建**: Maven 5模块

### 前端

- **框架**: Vue 3 + TypeScript
- **UI库**: Element Plus
- **构建工具**: Vite
- **状态管理**: Pinia
- **样式**: SCSS（深色科技风主题）

## 项目结构

### 后端模块（/Volumes/mac/code/java/ai_safe_library/）

```
ai-safe-library/
├── common/          # 公共工具类、常量
├── framework/       # 框架层：ES配置、Redis配置、Sa-Token配置
├── system/          # 系统管理：用户、部门、菜单、角色
├── business/        # 业务模块：风险线索、安全事件、风险报送
└── admin/           # 启动模块：Application入口
```

### 前端结构（/Volumes/mac/code/java/ai_safe_library_frontend/）

```
src/
├── api/             # API接口定义
├── views/
│   ├── risk/        # 业务页面：风险线索库、安全事件库、风险报送
│   ├── system/      # 系统管理页面
│   └── dashboard/   # 仪表盘
├── router/          # 路由配置
├── components/      # 公共组件（Sidebar、Layout）
└── stores/          # Pinia状态管理
```

## 核心业务模块

### 1. 风险线索库（Risk Clue）

- **ES索引**: `biz_risk_clue`
- **功能**: 线索采集、搜索、审核、导出
- **数据来源**: 人工录入、爬虫采集、自动化检测
- **风险等级**: critical, high, medium, info
- **审核状态**: 10=待审核, 20=已审核, 40=已驳回

**API接口**:

- `POST /business/risk-clue/search` - 搜索线索
- `GET /business/risk-clue/{id}` - 获取详情
- `PUT /business/risk-clue` - 创建/更新线索
- `POST /business/risk-clue/{id}/review` - 审核线索
- `GET /business/risk-clue/{id}/review-history` - 审核历史
- `GET /business/risk-clue/stats` - 统计数据

### 2. 安全事件库（Security Event）

- **ES索引**: 复用 `biz_risk_clue`（sourceType=security_event）
- **功能**: 安全事件查看、搜索、导出（只读）
- **特点**: 无需审核流程，直接展示

**API接口**:

- `GET /business/risk-event/search` - 搜索事件
- `GET /business/risk-event/{id}` - 获取详情

### 3. 风险报送（Risk Report）

- **功能**: 批量导入风险线索、按报送单位导出
- **数据处理**: Excel解析 → ES批量写入
- **数据隔离**: 按部门名称（deptName）隔离

**API接口**:

- `POST /business/risk-report/search` - 搜索报送记录
- `POST /business/risk-report/upload` - 批量上传Excel
- `GET /business/risk-report/export` - 按单位导出

## 数据库设计

### MySQL表（ai_safe_library）

- **sys_user** - 用户表
- **sys_dept** - 部门表（树形结构）
- **sys_role** - 角色表
- **sys_menu** - 菜单表
- **biz_tag_category** - 分类标签表（80条初始数据）

### ES索引

- **biz_risk_clue** - 风险线索（包含安全事件）
- **biz_risk_review_record** - 审核记录

## 配置信息

### 环境配置

- **后端端口**: 8080 (context-path: /api)
- **前端端口**: 5173
- **MySQL**: localhost:3306, root/123456, db=ai_safe_library
- **ES**: [https://localhost:9200](https://localhost:9200), elastic/V4h_Am00B-eNpeE5OSf* (自签名)
- **Redis**: localhost:6379, 无密码

### 认证配置

- **Sa-Token JWT secret**: AiSafeLibrary2026SecretKeyForJWT
- **Token格式**: `Bearer <tokenValue>`
- **登录接口**: POST /api/auth/login → {code, msg, data: {tokenValue}}

## 重要技术决策

1. **无Lombok**: JDK 17.0.19 + Lombok 1.18.32 存在兼容性问题，所有Java实体类使用手写getter/setter
2. **CorsFilter优先**: 使用CorsFilter Bean而非WebMvcConfigurer.addCorsMapping，因为Sa-Token拦截器会拦截OPTIONS预检请求
3. **ES Repository位置**: `@EnableElasticsearchRepositories` 必须放在 business 模块内，不能放在 framework 或 admin 模块
4. **Maven多模块构建**: `mvn spring-boot:run -pl` 必须先执行 `mvn install`，否则其他模块的类不在classpath
5. **数据隔离**: 风险报送按部门名称（deptName）隔离，而非部门ID

## 前端UI规范

- **主题**: 深色科技风（Dark Sci-Fi）
- **主色调**: 深蓝 #1e293b（侧边栏）
- **特效**: 玻璃拟态（Glassmorphism）、微光效果、高信息密度
- **布局**: 卡片网格优于表格，支持动画交互

## 开发注意事项

### 后端

- Spring Cache `@CacheEvict` 的SpEL表达式避免使用 `#root.module`，改用 `#参数名` 或 `allEntries=true`
- ES字段使用驼峰命名（camelCase）
- 审核状态码：10=待审核, 20=已审核, 40=已驳回

### 前端

- API返回格式：`{total, rows}` 或 `{records/list/data}`
- 组件使用 Element Plus 图标（CircleCloseFilled，无Skull图标）
- 路由路径：/risk/clue, /risk/event, /risk/report

## 常用命令

```bash
# 后端启动（必须先install）
cd /Volumes/mac/code/java/ai_safe_library
mvn clean compile -q
mvn install -DskipTests -q
mvn spring-boot:run -pl ai-safe-library-admin -DskipTests

# 前端启动
cd /Volumes/mac/code/java/ai_safe_library_frontend
npm run dev

# 测试API
python3 /tmp/test_all_apis.py
```

## 已知问题

1. DELETE /system/tag/{id} 可能返回500（预存问题，非当前范围）
2. Vision API Key可能过期，导致截图分析失败

---

**文档版本**: 1.0  
**最后更新**: 2026-05-20