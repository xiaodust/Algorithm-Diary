# Algorithm-Diary 算法刷题伴学助手

一个辅助 leetcode.cn 刷题的本地伴学工具。用户仍在 LeetCode 上做题，工具负责记录、沉淀、计划、推荐、解析与陪聊。

## 技术栈

- 后端：Java 21 + Spring Boot 3 + Maven + SQLite
- 前端：React 18 + Vite + Tailwind CSS
- LeetCode：leetcode.cn GraphQL
- AI：OpenAI 兼容 Chat API（支持流式输出）

## 核心能力

- **题单管理**：内置 Hot 100、面试经典 150、剑指 Offer 等官方题单，可切换主线题单并查看进度与预计完成时间
- **自定义题单**：自建题单，支持手动输入 slug / 从已做题目勾选 / 搜索题目 / 一键导入官方题单（LeetCode 75、SQL 50、动态规划等 14 个）
- **AI 算法助教**：右侧滑出式聊天侧边栏，随时提问错题、薄弱点、学习规划；支持多会话（新建/切换/删除，记录相互隔离）、SSE 流式输出、Markdown 与代码块渲染、面板宽度可拖拽拉伸
- **Agent 长期记忆**：统计画像 + 跨会话长期记忆（LLM 自动提炼 + 手动 ⭐ 记住），助教跨对话记得你的目标与弱点
- **引导式解析**：题目解析整合进 AI 助教侧边栏，实时拉取题目信息，支持方向提示/关键提示/完整思路三个等级
- 刷题目标：支持“完成当前题单”或“自定义题数”，并可设置每日目标题数
- 错题与复习：错题自动识别、复盘笔记、间隔重复复习、到期复习提醒
- 题型分析：薄弱题型、强项题型、AC 率、熟练度、遗忘率、题型趋势图
- 每日计划：按题单内错题、复习、薄弱新题、新题优先级自动生成任务
- 相似题推荐：规则推荐 + AI 动态推荐，失败时自动回退到规则方案
- AI 周报：支持缓存、手动刷新、每周一 8 点自动刷新

## 桌面端与安装包

项目支持以 JavaFX 原生窗口运行，不再依赖命令行启动。Windows 安装包可从 Release 下载：

- [Latest Release](https://github.com/xiaodust/Algorithm-Diary/releases/latest)

本地构建安装包：

```powershell
cd backend
mvn.cmd -Pdesktop -DskipTests package
```

构建完成后，使用 `jpackage` 生成 `AlgorithmDiary-<version>.exe`。桌面端启动后会自动在后台运行 Spring Boot 服务，并在系统默认浏览器中打开题目链接。

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

### 2. 配置 AI 模型（可选，使用 AI 周报 / 助教对话需要）

在应用右上角「AI 配置」中填入 OpenAI 兼容接口的 API Key、Base URL 与模型名（默认 `https://api.deepseek.com/v1`）。未配置时，周报与助教自动降级为规则回复，功能不中断。

### 3. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

### 4. 启动前端

```powershell
cd frontend
npm.cmd install --cache .\.npm-cache
npm.cmd run dev
```

浏览器打开 `http://localhost:3005`。

## 测试与构建

后端测试：

```powershell
cd backend
mvn.cmd test
```

前端测试与构建：

```powershell
cd frontend
npm.cmd test
npm.cmd run build
```

GitHub Actions 会在推送到 `main` 时自动执行后端测试、前端构建和桌面 jar 打包；推送 `v*` 标签时会自动生成 Windows 安装包并发布 Release。

## 版本记录

- **v0.3.2**：侧边栏支持宽度拖拽拉伸；修复开关侧边栏的滚动残留（fixed + transform 平移）
- **v0.3.1**：AI 助教交互优化——左右分栏布局（无蒙层）、Markdown/代码块渲染、Header 开关入口、ErrorBoundary 兜底
- **v0.3.0**：AI 算法助教上线——多会话聊天、SSE 流式输出、跨会话记忆沉淀、题目解析整合进侧边栏（并修复解析报错）
- **v0.2.9**：自定义题单（增删改 + 四种添加题目方式）
- **v0.2.8**：历史版本

## 当前进度

- 后端核心逻辑：题单、自定义题单、目标、错题、题型、薄弱点分析、每日计划、相似题推荐、AI 助教多会话、Agent 记忆（画像 + 长期事实）
- LeetCode 客户端：leetcode.cn GraphQL（提交记录、已做题目、题目搜索、题目详情、官方题单拉取），带重试和请求节流
- 前端仪表盘：主线题单进度、自定义题单管理、目标设置、今日计划、薄弱题型与趋势、推荐、错题本、AI 周报、AI 助教侧边栏
- 测试：Service 单元测试、Controller 集成测试、React 组件测试
- 桌面端：JavaFX 原生窗口、托盘、题目链接跳转系统浏览器
- CI/CD：GitHub Actions 自动测试、打包与发布

## 目录

- `backend/`：Spring Boot 服务
- `frontend/`：React 界面
- `docs/`：功能方案书（本地文档，不入库）
- `.github/workflows/`：CI/CD 工作流
