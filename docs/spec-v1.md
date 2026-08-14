# Spec: 算法征服者（Algorithm-Diary 升级版）

## 目标（Objective）

给以 LeetCode 为主的刷题者做一个本地 Web 工具，把枯燥的刷题变成有成就感的“征服”体验，并用 Agent 分析长期刷题记忆，让“每日任务 → 刷题 → 复盘 → 强弱项分析 → 推荐下一题”形成自动闭环。

目标用户：单用户、长期刷算法题、需要外部激励和复习机制、主要使用 LeetCode 的开发者。

## 核心理念

把隐性的进步变成显性的征服：

- 征服地图：主题科技树，每 AC 一题点亮一块区域
- 熟练度五级：未碰过 / 看懂题解 / 独立 AC / 一次通过且讲得清 / 能优化或变形
- 每日 Boss 战：每日一题 + 连胜 combo
- 三层目标：长期愿景 → 阶段里程碑 → 每日核心任务 + 冲刺任务
- Agent 记忆分析：强弱项、遗忘、题目推荐、日报/周报

## MVP 功能范围

1. LeetCode 数据同步：提交记录、题目元数据、每日一题
2. 征服地图：按主题排列的科技树 + 点亮动画
3. 每日 Boss 战：按优先级自动组题 + 打卡 + 连胜记录
4. 长期 / 每日目标三层体系
5. 错题复盘 + 间隔重复复习
6. 题解思路卡片（本地 Markdown）
7. Agent 记忆分析：强弱项、遗忘分析、题目推荐、简报

## Agent 记忆分析设计

### 数据模型（刷题记忆）

- `problems`：题目元数据（id、title、slug、difficulty、tags、url）
- `submissions`：提交记录（problem_id、submitted_at、status、language、runtime、memory）
- `problem_states`：每题状态（mastery_level 0-4、ac_count、attempt_count、first_ac_at、last_review_at、next_review_at、is_mistake、mistake_type）
- `reviews`：复习记录（problem_id、reviewed_at、passed、notes）
- `daily_logs`：每日打卡（date、core_task、bonus_task、completed、duration、mood）
- `goals`：长期 / 阶段目标（title、target、progress、deadline）
- `notes`：题解 Markdown（problem_id、path、created_at、updated_at）
- `insights`：Agent 分析结果缓存（generated_at、type、content_json、prompt_version）

### 分析流水线

1. **确定性统计（代码 / SQL，可测试）**
   - 按 tag 聚合 AC 率、平均尝试次数、平均耗时、熟练度均值、遗忘率
   - 强项 = AC 率高 + 熟练度高 + 遗忘率低（有样本量门槛，避免一道题就下结论）
   - 薄弱 = AC 率低 + 尝试次数多 + 熟练度低，或复习时频繁遗忘
   - 遗忘检测：`next_review_at` 过期，或复习未通过
2. **LLM 解读（Agent）**
   - 输入：结构化统计摘要 + 最近 30 天记录 + 用户长期目标
   - 输出：自然语言洞察（例如“你的 DP 在背包类题上连续 5 次 WA”）、3-5 道具体推荐题及理由、下周计划建议
3. **推荐落地**
   - 从 LeetCode 题库按薄弱 tag + 合适难度 + 未做过 + 高频度筛选，附题目 URL
4. **反馈闭环**
   - 用户对推荐点赞 / 跳过 / 标记“太难”，反馈写回数据，Agent 下次调整

## 技术栈（默认建议，待确认）

- 后端：Python 3.12 + FastAPI（数据分析与 LLM 生态成熟；备选 Spring Boot 3 + Java 21）
- 前端：React 18 + Vite + Tailwind CSS（本地 dashboard）
- 存储：SQLite + 本地 Markdown 题解
- LLM：OpenAI API，用于 Agent 分析解读与推荐
- LeetCode：官方 GraphQL 接口

## 命令（Commands）

后端（默认 Python 方案）：

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload
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
pytest
ruff check .
```

## 项目结构（Project Structure）

```
Algorithm-Diary/
  backend/
    app/
      main.py              # FastAPI 入口
      db.py                # SQLite 连接与迁移
      models.py            # 数据模型
      leetcode.py          # LeetCode GraphQL 客户端
      analyzer.py          # 确定性统计
      agent.py             # LLM 解读与推荐
      scheduler.py         # 每日任务与复习调度
      routes/
        sync.py            # 同步与题目
        dashboard.py       # 地图、统计、打卡
        goals.py           # 长期 / 每日目标
        review.py          # 错题与复习
        insights.py        # Agent 分析
    tests/
  frontend/
    src/
      components/
      pages/
  data/
    algo.db                # SQLite（不提交 git）
    notes/                 # Markdown 题解
  docs/
    spec.md
  tasks/
    plan.md
    todo.md
  二叉树/                  # 现有 Java 练习代码，保留或迁移
  Day1/                    # 现有练习日记，保留或迁移
```

## 代码风格（Code Style）

Python 后端：类型注解 + snake_case，示例：

```python
from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class Submission:
    problem_id: int
    status: str
    submitted_at: datetime


def is_accepted(submission: Submission) -> bool:
    return submission.status == "Accepted"
```

前端：函数组件 + hooks，组件按页面和可复用组件拆分。

## 测试策略（Testing Strategy）

- 框架：pytest
- 测试位置：`backend/tests/`
- 重点覆盖：`analyzer.py`（强弱项与遗忘判定）、`scheduler.py`（复习调度）、`leetcode.py`（接口解析）
- 确定性统计逻辑必须有单元测试；LLM 输出用固定样例做快照 / 结构校验，不追求逐字一致
- 前端 MVP 以手动验收为主，后续再补组件测试

## 边界（Boundaries）

Always（始终）：

- LEETCODE_SESSION 只存本地，绝不写入 git 或日志
- 确定性统计逻辑必须可测试
- 错题、复习、熟练度变化必须有对应测试
- 删除数据前先备份

Ask first（先问）：

- 更换技术栈或引入新的重型依赖
- 增加云同步、多设备或多人功能
- 调用 LLM API 产生费用
- 修改数据库 schema 或迁移已有练习数据

Never（绝不）：

- 提交任何 token / cookie / 密钥
- 把用户刷题数据上传到第三方服务
- 无备份地删除现有 `Day1`、`二叉树` 等练习内容

## 成功标准（Success Criteria）

- 能同步 LeetCode 提交记录，并据此点亮征服地图
- 每日任务按“错题 > 到期复习 > 薄弱新题”自动生成
- Agent 能生成一份强弱项报告，并给出 3-5 道可点击的推荐题
- 打卡、熟练度、复习队列能正确更新并持久化
- 长期目标进度随刷题自动推进

## 开放问题（Open Questions）

以下问题我会先按推荐值执行，你确认或修改即可：

1. **现有仓库怎么处理**：把 `Algorithm-Diary` 升级成新工具（推荐），还是新建独立目录、旧仓库只做题解归档
2. **技术栈**：Python FastAPI（推荐，轻快）／ Spring Boot（贴合你的 Java 背景）／ Node + Next
3. **LeetCode 数据来源**：用 `LEETCODE_SESSION` cookie 自动同步（推荐，体验最好）／ 手动录入
4. **题解的主次**：自己写复盘笔记（推荐，沉淀最有效）／ 自动拉取官方或社区题解 ／ AI 生成思路
5. **错题判定**：自动按 WA / TLE 判定（推荐）／ 手动标记“未独立做出”
6. **刷题主要目标**：面试（高频题、剑指 Offer）／ 算法能力提升 ／ 竞赛
7. **Agent 集成方式**：应用内调用 OpenAI API（推荐，自包含）／ 通过 Codex 任务按需分析 ／ 两者结合；同时确认是否接受 API 调用费用
