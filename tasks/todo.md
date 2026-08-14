# 任务清单

## 后端骨架与数据层

- [ ] Task: 创建 Maven 工程与核心依赖
  - Acceptance: `mvn -q -DskipTests package` 成功
  - Verify: `cd backend && mvn -q -DskipTests package`
  - Files: `backend/pom.xml`

- [ ] Task: 编写 SQLite 配置与 schema
  - Acceptance: 应用启动后自动建表，重复启动不报错
  - Verify: `mvn spring-boot:run` 观察日志，`data/algo.db` 生成
  - Files: `backend/src/main/resources/application.yml`, `schema.sql`

- [ ] Task: 建立核心模型与 DAO
  - Acceptance: 题单、题目、状态、错题、复习可增删查改
  - Verify: 单元测试覆盖 DAO 基础操作
  - Files: `backend/src/main/java/com/algodiary/model/*`, `repository/*`

## LeetCode 客户端与同步

- [ ] Task: 实现 LeetCode GraphQL 客户端
  - Acceptance: 能构造带认证头的请求，正确解析 fixture 响应，失败时退避重试
  - Verify: `LeetCodeClientTest` 使用 fixture 不联网
  - Files: `backend/src/main/java/com/algodiary/leetcode/LeetCodeClient.java`

- [ ] Task: 实现同步服务
  - Acceptance: 从 fixture 的 `userProgressQuestionList` 与提交列表写库，增量更新不重复
  - Verify: `SyncServiceTest`
  - Files: `backend/src/main/java/com/algodiary/service/SyncService.java`

## 题单与目标

- [ ] Task: 内置题单种子与主线设置
  - Acceptance: Hot 100 / 剑指 Offer / 面试经典 150 可加载，用户可设置主线
  - Verify: `ProblemListServiceTest`
  - Files: `service/ProblemListService.java`, `resources/lists/*.json`

## 错题、题型与计划

- [ ] Task: 错题判定与复习队列
  - Acceptance: WA/TLE 自动判为错题，复盘可保存，到期复习可查询
  - Verify: `MistakeServiceTest`
  - Files: `service/MistakeService.java`

- [ ] Task: 题型打标与薄弱点统计
  - Acceptance: 题目有题型标签，能按题型输出 AC 率、熟练度、遗忘率
  - Verify: `AnalyzerServiceTest`
  - Files: `service/TopicService.java`, `service/AnalyzerService.java`

- [ ] Task: 每日计划生成
  - Acceptance: 严格按“题单内错题 > 题单内复习 > 题单内薄弱新题 > 其他”出题
  - Verify: `PlannerServiceTest`
  - Files: `service/PlannerService.java`

## 推荐与解析

- [ ] Task: 相似题推荐
  - Acceptance: 按题型和难度推荐，题单内优先，返回 leetcode.cn 链接
  - Verify: `RecommendationServiceTest`
  - Files: `service/RecommendationService.java`

- [ ] Task: 引导式解析（LLM 可选）
  - Acceptance: 无 API key 时返回规则提示，有 key 时返回分级解析
  - Verify: `ExplainServiceTest`
  - Files: `service/ExplainService.java`, `service/LlmClient.java`

## REST API

- [ ] Task: 暴露核心 REST 接口
  - Acceptance: 同步、题单、计划、错题、题型、推荐、解析接口可用
  - Verify: `@WebMvcTest` 或手动 `curl`
  - Files: `controller/*.java`

## 前端

- [ ] Task: 初始化 React + Vite + Tailwind
  - Acceptance: `npm run dev` 启动，页面渲染
  - Verify: 浏览器打开 localhost
  - Files: `frontend/package.json`, `src/*`

- [ ] Task: 最小仪表盘
  - Acceptance: 显示今日计划、主线题单进度、连胜、薄弱题型
  - Verify: 连接后端 API 手动验收
  - Files: `frontend/src/pages/Dashboard.tsx`

- [ ] Task: 错题与推荐页面
  - Acceptance: 可看错题、标记复盘、查看推荐并跳转 leetcode.cn
  - Verify: 手动验收
  - Files: `frontend/src/pages/Mistakes.tsx`, `Recommendations.tsx`
