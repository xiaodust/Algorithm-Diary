# 实施计划（Algorithm-Diary 伴学助手）

## 技术决策

- 后端：Java 21 + Spring Boot 3.x + Maven
- 数据库：SQLite，使用 sqlite-jdbc + Spring JDBC（JdbcTemplate），schema 由 `schema.sql` 初始化
- 前端：React 18 + Vite + Tailwind CSS
- LeetCode：leetcode.cn GraphQL，认证使用 `LEETCODE_SESSION` + `csrftoken`，带浏览器 UA 和 CSRF 头
- LLM：通过 WebClient 调用 OpenAI API，`OPENAI_API_KEY` 未配置时降级为规则实现

## 关键接口结论

- GraphQL 端点：`https://leetcode.cn/graphql/noj-go`，回退 `https://leetcode.cn/graphql/`
- 已做题目：`userProgressQuestionList`，返回 `frontendId`、`title`、`titleSlug`、`questionStatus`、`lastResult`
- 全部提交：`submissionList(offset, limit, lastKey, questionSlug, lang, status)`，返回 `submissions[]`
- 每日一题：`activeDailyCodingChallengeQuestion`
- 题目详情：`questionData(titleSlug)`，含难度、官方标签
- 限流：约 60 请求 / 10 分钟，需增量同步、节流、退避重试

## 实现顺序（垂直切片）

1. 数据层：`pom.xml`、`application.yml`、`schema.sql`、模型与基础 DAO
2. LeetCode 客户端：HTTP/GraphQL 封装、认证头、限流重试
3. 同步：`userProgressQuestionList` + 提交记录，落库为题目和状态
4. 题单：内置 Hot 100 / 剑指 Offer / 面试经典 150，支持设置主线
5. 错题与复习：错题判定、复盘、间隔重复队列
6. 题型与薄弱点：题型打标、聚合统计
7. 每日计划：题单优先的自动组题
8. 相似题推荐：规则 + 可选 LLM 理由
9. 引导式解析：可选 LLM 分级提示
10. 最小前端：仪表盘、错题、题型、推荐、解析入口

## 风险与对策

- leetcode.cn 限流：增量同步 + 请求间隔 + 指数退避 + 缓存上次同步位置
- Cloudflare/Django CSRF：完整携带 cookie、x-csrftoken、Origin、Referer、浏览器 UA
- SQLite 并发写入：单连接或串行化写操作，MVP 阶段避免并发写
- 题单数据来源不稳定：先内置快照，后续验证 `problem-list` 接口再增强
- 测试依赖真实登录态：LeetCode 客户端解析用 fixture 测试，不依赖在线请求

## 验证检查点

- 数据层启动成功，schema 可重复初始化
- LeetCode 客户端能正确构造请求、解析 fixture 响应
- 同步服务把 fixture 数据写库并更新状态
- 每日计划严格按题单优先级产出任务
- 薄弱点统计与推荐在样例数据上结果正确
