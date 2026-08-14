# Spec: 算法刷题伴学助手（v2，Spring Boot / leetcode.cn）

## 目标（Objective）

做一个辅助 LeetCode 刷题的本地伴学工具。用户始终在 leetcode.cn 上刷题，本工具只负责：

- 记录刷题记录、错题、卡点
- 长时间沉淀题型与薄弱点
- 围绕用户指定的题单生成每日 / 长期刷题计划
- 给出刷题建议和相似题推荐，优先推荐当前题单内的题
- 在需要时提供引导式题目解析
- 用连胜、熟练度、错题毕业等机制提供成就感

一句话定位：**LeetCode 负责做题，工具负责“记得住、练得准、坚持得下去”。**

## 核心理念

- 用户刷题仍然在 leetcode.cn，工具只做辅助
- 题单是主线：用户说“我要刷 Hot 100”，工具就围绕该题单规划、推荐和统计
- 把隐性进步变成显性反馈：熟练度、连胜、错题毕业、成长曲线

## 用户画像与使用流程

单用户、主要用 leetcode.cn、长期刷算法题的开发者。

典型一天：

1. 设置或切换目标题单（例如“Hot 100”），工具把该题单设为主线
2. 打开工具，看今天的计划（题单内错题 / 薄弱题型 / 未做题），点链接跳转 leetcode.cn
3. 在 leetcode.cn 刷题，卡住时回工具标记并获取引导式提示
4. 完成后工具同步提交记录，判定错题和熟练度
5. 晚上复盘错题，写“卡在哪、下次怎么办”
6. 工具更新题单进度、连胜、薄弱点，生成明天的计划和建议

## 核心模块

### 1. 题单与长期目标（核心）

- 内置常见题单：LeetCode Hot 100、剑指 Offer、面试经典 150
- 支持用户自定义题单、手动导入题目列表
- 用户可设置一个“主线题单”，也可以保留多个题单并随时切换
- 长期目标默认等于主线题单完成度（如 Hot 100 完成 68/100）
- **题单优先规则**：只要用户设置了目标题单，以下逻辑都优先从该题单中选题：
  - 每日计划：题单内错题 > 题单内到期复习 > 题单内未做题（按薄弱题型优先）> 题单外到期复习 > LeetCode 每日一题
  - 相似题推荐：优先推荐题单内、同题型、难度递进的题
  - 进度统计：主线题单完成度始终可见

### 2. 数据采集与同步（leetcode.cn）

- 通过 leetcode.cn GraphQL 接口同步提交记录（Accepted / Wrong Answer / TLE 等、语言、时间）
- 同步每日一题、题目元数据（难度、官方标签）
- 手动快速标记：卡住了 / 看了题解 / 独立完成
- 后续可选：浏览器扩展在 leetcode.cn 页面悬浮“卡住了”按钮

### 3. 错题本（核心）

- 判定来源：自动按 WA / TLE 判定 + 用户手动标记“没独立做出来”
- 错误类型：思路错 / 边界错 / 超时 / 语法错
- 复盘模板：卡在哪一步、为什么没想到、下次怎么避免、相似题
- 错题进入间隔重复队列，定期重刷直到“毕业”（连续两次独立 AC）

### 4. 题型记录与薄弱点

- 细粒度题型体系：
  - 数据结构：数组、链表、栈/队列、哈希表、树、堆、图、Trie、并查集
  - 算法模式：双指针、滑动窗口、二分、递归/回溯、DFS/BFS、DP、贪心、排序、位运算、前缀和、单调栈
- 每道题打一个或多个题型标签：官方标签 + LLM 辅助分类 + 用户手动调整
- 按题型聚合 AC 率、平均尝试次数、熟练度、题量、遗忘率
- 薄弱题型 = AC 率低 + 尝试多 + 熟练度低 + 频繁遗忘（设样本量门槛）

### 5. 相似题推荐

- 针对薄弱题型，优先从主线题单中推荐同题型、难度递进的题
- 推荐理由用自然语言说明“为什么推这题”
- 推荐结果附 leetcode.cn 题目链接，点击直接跳转
- 用户点赞 / 跳过 / 标记“太难”，反馈回流调整下次推荐

### 6. 每日计划

- 按题单优先规则自动组题，默认核心任务 1-2 道 + 冲刺任务可选
- 每条任务附 leetcode.cn 链接
- 完成后根据提交记录自动打卡，支持连胜记录

### 7. 引导式题目解析

- 分级提示：方向提示 → 关键转换或边界提示 → 完整思路
- 引导用户自己想出来，而不是直接给答案，最终仍在 leetcode.cn 提交
- 用户请求解析时记录一次“卡点事件”，供薄弱点分析使用

### 8. 成就感机制

- 连续打卡天数与里程碑
- 题型熟练度升级（每个题型有进度条 / 等级）
- 题单进度条（例如 Hot 100 完成度）
- 错题“毕业”提醒
- 成长曲线：从“看题解”到“独立 AC”再到“秒杀”
- 周报：本周征服了哪些题型、哪些从薄弱变强、错题毕业数量

## 技术栈

- 后端：Java 21 + Spring Boot 3.x + Maven（用户指定）
- 数据库：SQLite（sqlite-jdbc + Spring JDBC / JdbcTemplate）
- 前端：React 18 + Vite + Tailwind CSS（本地伴学面板）
- LLM：OpenAI API（Java 侧通过 WebClient 调用，或引入 Spring AI）
- LeetCode：leetcode.cn GraphQL 接口，登录态使用 LEETCODE_SESSION cookie
- 后续可选：浏览器扩展

## 数据模型（刷题记忆）

- `problems`：题目元数据（id、slug、title、difficulty、official_tags、url）
- `problem_lists`：题单（id、name、slug、source、description）
- `problem_list_items`：题单条目（list_id、problem_id、position）
- `user_goals`：当前主线题单与目标（list_id、target_type、target、progress）
- `topics`：题型体系（id、name、category、parent）
- `problem_topics`：题目与题型多对多
- `submissions`：同步的提交记录（problem_id、status、language、submitted_at）
- `problem_states`：每题状态（mastery_level、ac_count、attempt_count、first_ac_at、last_review_at、next_review_at、is_mistake、mistake_type）
- `mistakes`：错题复盘（problem_id、error_type、stuck_point、lesson、similar_problems）
- `stuck_events`：卡点事件（problem_id、happened_at、source、hint_level）
- `reviews`：复习记录（problem_id、reviewed_at、passed、notes）
- `daily_plans`：每日计划（date、core_tasks、bonus_tasks、completed、streak）
- `notes`：题解 / 思路卡片 Markdown
- `recommendations`：推荐缓存与用户反馈
- `insights`：Agent 分析结果缓存

## 分析流水线（Agent）

1. **确定性统计（Java / SQL，可测试）**
   - 题型维度 AC 率、尝试次数、熟练度、遗忘率
   - 薄弱 / 强项题型判定
   - 题单完成度与进度
   - 错题复习队列与到期检测
2. **LLM 解读**
   - 输入：结构化统计摘要 + 最近记录 + 主线题单 + 用户长期目标
   - 输出：自然语言洞察、相似题推荐及理由、解析提示、周报
3. **推荐落地**
   - 从主线题单开始筛选：题型 + 难度 + 未做过，题单不足再扩展题单外
   - 生成可点击的 leetcode.cn 链接
4. **反馈闭环**
   - 推荐反馈、解析使用情况、复习结果写回数据，影响下次分析

## 命令（Commands）

后端（Spring Boot / Maven）：

```powershell
cd backend
mvn spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

测试与检查：

```powershell
cd backend
mvn test
```

## 项目结构（Project Structure）

```
Algorithm-Diary/
  backend/
    pom.xml
    src/main/java/com/algodiary/
      AlgorithmDiaryApplication.java
      config/
      controller/
      service/
      repository/
      model/
      leetcode/              # leetcode.cn GraphQL 客户端
      agent/                 # LLM 解析、推荐、周报
    src/main/resources/
      application.yml
      schema.sql
    src/test/java/
  frontend/
    src/
      components/
      pages/
  data/
    algo.db
    notes/
  docs/
    spec.md
    spec-v1.md               # 第一版，保留参考
  tasks/
    plan.md
    todo.md
  .gitignore
  README.md
```

## 代码风格（Code Style）

Java：标准 Spring Boot 分层，示例：

```java
public record Submission(
        String problemId,
        String status,
        Instant submittedAt
) {
    public boolean isAccepted() {
        return "Accepted".equals(status);
    }
}
```

前端：函数组件 + hooks，组件按页面和可复用组件拆分。

## 测试策略（Testing Strategy）

- 框架：JUnit 5 + Mockito / Spring Boot Test
- 位置：`backend/src/test/java/`
- 重点：分析统计（薄弱点与遗忘）、计划与复习调度、leetcode.cn 接口解析、题单优先级
- 确定性逻辑必须有单元测试；LLM 输出用固定样例做结构校验，不追求逐字一致

## 边界（Boundaries）

Always：

- LEETCODE_SESSION 只存本地，绝不写入 git 或日志
- 确定性统计逻辑必须可测试
- 错题、复习、熟练度、题单进度变化必须有对应测试
- 删除数据前先备份

Ask first：

- 更换技术栈或引入新的重型依赖
- 增加云同步、多设备、浏览器扩展等新形态
- 调用 LLM API 产生费用
- 修改数据库 schema 或迁移已有练习数据

Never：

- 提交任何 token / cookie / 密钥
- 把用户刷题数据上传到第三方服务
- 替代用户在 leetcode.cn 上提交代码

## MVP 范围

第一期：

1. leetcode.cn 提交记录同步 + 每日一题
2. 题单管理：内置 Hot 100 / 剑指 Offer / 面试经典 150，支持设置主线
3. 错题本 + 复盘 + 复习队列
4. 题型记录 + 薄弱点分析
5. 每日计划（题单优先）+ 打卡 + 连胜
6. 相似题推荐（题单优先，附 leetcode.cn 链接）
7. 引导式题目解析
8. 基础成就感（连胜、熟练度、错题毕业、题单进度、周报）

第二期：

- 浏览器扩展（leetcode.cn 页面内标记卡点）
- 更细的遗忘曲线与复习策略
- 更强的视觉反馈

## 成功标准（Success Criteria）

- 能同步 leetcode.cn 提交记录，自动更新错题和熟练度
- 用户设置主线题单后，每日计划和推荐都优先来自该题单
- 题单完成度、连胜、错题毕业、熟练度能正确更新并持久化
- 能按题型展示强弱项，并给出 3-5 道相似题推荐及理由
- 用户请求解析时能分级给出引导式提示，且不影响其在 leetcode.cn 提交

## 开放问题（Open Questions）

以下按推荐值先执行，你确认或修改即可：

1. **题单数据来源**：先内置维护 Hot 100 / 剑指 Offer / 面试经典 150 的快照（推荐，稳定可靠），同时验证 leetcode.cn 是否能直接同步官方题单
2. **卡点记录方式**：MVP 先靠同步 + 手动标记（推荐），浏览器扩展放第二期
3. **LLM 集成**：应用内调用 OpenAI API（推荐，自包含）／ 走 Codex 按需分析；是否接受 API 费用
4. **刷题目标**：默认按面试方向（题单已隐含），是否还需细分
