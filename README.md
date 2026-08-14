# Algorithm-Diary 算法刷题伴学助手

一个辅助 leetcode.cn 刷题的本地伴学工具。用户仍在 LeetCode 上做题，工具负责记录、沉淀、计划、推荐和解析。

## 技术栈

- 后端：Java 21 + Spring Boot 3 + Maven + SQLite
- 前端：React 18 + Vite + Tailwind CSS
- LeetCode：leetcode.cn GraphQL

## 运行

一键启动：

```powershell
cd D:\work\Algorithm-Diary
.\start.ps1
```

一键停止：

```powershell
.\stop.ps1
```

也可以分别手动启动：

### 1. 配置 LeetCode 登录态（可选，但同步提交记录需要）

在 `backend/src/main/resources/application.yml` 中或环境变量里配置：

```powershell
$env:LEETCODE_SESSION="<你的 cookie>"
$env:LEETCODE_CSRF_TOKEN="<csrftoken>"
```

如果没配置，工具仍可启动，但同步接口会因未登录失败。

### 2. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

### 3. 启动前端

```powershell
cd frontend
npm.cmd install --cache .\.npm-cache
npm.cmd run dev
```

浏览器打开 `http://localhost:3005`。

## 当前进度

- 后端核心逻辑：题单、错题、题型、薄弱点分析、每日计划、相似题推荐、引导式解析
- LeetCode 客户端：leetcode.cn GraphQL（提交记录、已做题目、每日一题、题目详情）
- 前端仪表盘：主线题单进度、今日计划、薄弱题型、推荐、错题本
- 单元测试覆盖核心纯逻辑与接口解析

## 目录

- `backend/`：Spring Boot 服务
- `frontend/`：React 界面
- `docs/spec.md`：当前规格
- `docs/spec-v1.md`：早期规格存档
- `tasks/plan.md`、`tasks/todo.md`：实施计划与任务清单
