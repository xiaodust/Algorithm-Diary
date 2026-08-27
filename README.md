# Algorithm-Diary 算法刷题伴学助手

一个辅助 leetcode.cn 刷题的本地伴学工具。用户仍在 LeetCode 上做题，工具负责记录、沉淀、计划、推荐和解析。

## 技术栈

- 后端：Java 21 + Spring Boot 3 + Maven + SQLite
- 前端：React 18 + Vite + Tailwind CSS
- LeetCode：leetcode.cn GraphQL

## 核心能力

- 题单管理：Hot 100、面试经典 150、剑指 Offer，可切换主线题单并查看进度与预计完成时间
- 刷题目标：支持“完成当前题单”或“自定义题数”，并可设置每日目标题数
- 错题与复习：错题自动识别、复盘笔记、间隔重复复习、到期复习提醒
- 题型分析：薄弱题型、强项题型、AC 率、熟练度、遗忘率、题型趋势图
- 每日计划：按题单内错题、复习、薄弱新题、新题优先级自动生成任务
- 相似题推荐：规则推荐 + AI 动态推荐，失败时自动回退到规则方案
- 引导式解析：无 LLM Key 时使用规则提示，配置 LLM 后使用分级解析
- Agent 长期记忆：持久化用户刷题画像，用于长期分析和推荐
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

## 当前进度

- 后端核心逻辑：题单、目标、错题、题型、薄弱点分析、每日计划、相似题推荐、引导式解析、Agent 记忆
- LeetCode 客户端：leetcode.cn GraphQL（提交记录、已做题目、每日一题、题目详情），带重试和请求节流
- 前端仪表盘：主线题单进度、目标设置、今日计划、薄弱题型与趋势、推荐、错题本、AI 周报
- 测试：Service 单元测试、Controller 集成测试、React 组件测试
- 桌面端：JavaFX 原生窗口、托盘、题目链接跳转系统浏览器
- CI/CD：GitHub Actions 自动测试、打包与发布

## 目录

- `backend/`：Spring Boot 服务
- `frontend/`：React 界面
- `.github/workflows/`：CI/CD 工作流
