# 三大核心模块开发计划

## 确认决策
- 审核记录：ES 存储（biz_risk_review_record 索引）
- report_unit：用部门名称做数据隔离（查 sys_dept.deptName）
- review_status：10=待审核, 20=已审核, 40=已驳回
- 前端风格：Element Plus + 自定义 SCSS 实现深色科技风

## Phase 1: 后端 - ES 文档模型
- [ ] BizRiskClue ES document 类（映射 biz_risk_clue 索引）
- [ ] BizRiskReviewRecord ES document 类（映射 biz_risk_review_record 索引）
- [ ] ES Repository 接口

## Phase 2: 后端 - RiskClueService
- [ ] 搜索/分页/筛选（时间/来源/标签/风险等级/状态）
- [ ] 部门数据隔离（report_unit = 当前用户部门名称）
- [ ] CRUD 基础操作

## Phase 3: 后端 - RiskReviewService
- [ ] 审核逻辑（审核→更新线索状态+写审核记录）
- [ ] 审核历史查询

## Phase 4: 后端 - RiskReportService
- [ ] Excel 批量解析
- [ ] 批量写入 ES（source_type=report, review_status=10, report_unit=当前用户单位）

## Phase 5: 后端 - Controller 层
- [ ] RiskClueController（线索库 CRUD + 审核）
- [ ] SecurityEventController（事件库只读查询）
- [ ] RiskReportController（报送列表 + 批量上传）

## Phase 6: 前端 - API 层
- [ ] riskClue.ts（搜索/详情/审核/导出）
- [ ] securityEvent.ts（查询/详情）
- [ ] riskReport.ts（报送列表/批量上传）

## Phase 7: 前端 - 风险线索库页面
- [ ] 左侧筛选面板（时间/来源/标签/风险等级/状态）
- [ ] 中间卡片流列表（深色科技风）
- [ ] 右侧审核 Drawer（是否有害/标签多选/原因/意见）

## Phase 8: 前端 - 安全事件库页面
- [ ] Table 只读列表
- [ ] 筛选条件
- [ ] 详情 Drawer

## Phase 9: 前端 - 风险报送页面
- [ ] Tab: 我的报送列表
- [ ] Tab: 批量上传 Excel

## Phase 10: 前端 - 路由+菜单
- [ ] Sidebar 添加风险线索库/安全事件库/风险报送菜单
- [ ] Router 配置

## Phase 11: 编译验证
- [ ] mvn compile 通过
- [ ] vue-tsc --noEmit 通过
