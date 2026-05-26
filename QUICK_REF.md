# AI安全情报平台 - 快速参考

## 项目路径
- 后端: `/Volumes/mac/code/java/ai_safe_library/`
- 前端: `/Volumes/mac/code/java/ai_safe_library_frontend/`

## 核心技术
- Spring Boot 3.2.5 + Java 17 + Sa-Token + MyBatis Plus + ES 8.14.3 + Redis
- Vue 3 + Element Plus + Vite + TypeScript + Pinia + SCSS

## 端口与地址
- 后端: http://localhost:8080/api
- 前端: http://localhost:5173
- MySQL: localhost:3306 (root/123456, db=ai_safe_library)
- ES: https://localhost:9200 (elastic/V4h_Am00B-eNpeE5OSf*)
- Redis: localhost:6379

## 认证
- 登录: POST /api/auth/login → {code, msg, data: {tokenValue}}
- 请求头: Authorization: Bearer <token>

## 业务模块
1. 风险线索库: /business/risk-clue/* (ES索引: biz_risk_clue)
2. 安全事件库: /business/risk-event/* (只读，复用biz_risk_clue)
3. 风险报送: /business/risk/report/* (Excel导入/导出)

## 审核状态
- 10: 待审核
- 20: 已审核
- 40: 已驳回

## 启动命令
```bash
# 后端（必须先install）
mvn clean compile -q && mvn install -DskipTests -q && mvn spring-boot:run -pl ai-safe-library-admin -DskipTests

# 前端
npm run dev
```

## 重要规则
- 无Lombok，手写getter/setter
- ES Repository注解放在business模块
- 必须mvn install后才能spring-boot:run -pl
- CorsFilter Bean优先于WebMvcConfigurer
- 风险报送按deptName隔离数据

## 常见问题
- "Found 0 repositories" → 需要mvn install
- OPTIONS 403 → 检查CorsFilter配置
- Token无效 → 检查Bearer前缀

---
详见: [[ARCHITECTURE.md]]
