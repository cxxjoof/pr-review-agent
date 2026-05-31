# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。

用户输入 GitHub 仓库地址和 PR 编号后，系统会自动获取 PR 基本信息与变更文件，解析 diff 构建审查上下文，调用 OpenAI 兼容模型生成结构化 Review 结果，并输出以下内容：

- PR 变更总结
- 变更模块说明
- 结构化发现项列表
- Review 建议
- 测试建议
- 总体结论

当前版本已经支持 PR 类型识别、结构化发现项展示、可执行修改示例、GitHub diff 跳转和反馈闭环。

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [仓库结构](#仓库结构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [接口概览](#接口概览)
- [数据库说明](#数据库说明)
- [测试方式](#测试方式)
- [常见问题](#常见问题)
- [项目文档](#项目文档)
- [安全说明](#安全说明)
- [版本更新](#版本更新)

## 功能特性

- 支持提交公开 GitHub 仓库地址和 PR 编号，创建审查任务
- 支持 GitHub 仓库地址格式校验，兼容标准 HTTPS 仓库地址、带 `.git` 的克隆地址和 GitHub SSH 地址
- 支持从 GitHub REST API 获取 PR 标题、作者、分支、变更文件和 patch 内容
- 支持将 diff 解析为结构化审查上下文，而不是把原始 patch 全量传给模型
- 支持 PR 类型识别：`DOCUMENTATION`、`CODE`、`CONFIG`、`TEST`、`DEPENDENCY`、`CICD`、`MIXED`
- 支持通过 OpenAI 兼容 Chat Completion API 生成结构化 Review 报告
- 支持结构化发现项输出：`findingLevel`、`findingKind`、`findingCategory`
- 支持 `ADVISORY` 作为独立建议等级，降低文档类 PR 的误报和过度诊断
- 支持 `beforeExample`、`afterExample`、`suggestedPatch`、`diffUrl`
- 支持发现项反馈闭环：`USEFUL`、`FALSE_POSITIVE`、`IGNORED`、`FIXED`
- 支持首页产品化交互：能力说明、示例报告预览、最近一次分析记录
- 支持前端三阶段分析加载态：提交任务、获取 PR 信息并分析变更、生成并读取报告
- 支持前端卡片式发现项展示，并支持展开详情、反馈操作和 GitHub diff 跳转
- 支持展开详情时保持当前位置，避免页面自动滚动导致按钮偏移
- 支持统一错误响应，对参数错误、上游调用失败、超时等场景给出清晰提示

## 技术栈

### 后端

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring WebFlux `WebClient`
- Spring Data JPA
- Maven
- MySQL
- H2 内存数据库（用于本地快速启动）

### 前端

- React
- Vite
- Ant Design
- Axios

### 外部服务

- GitHub REST API
- OpenAI 兼容模型 API

## 系统架构

系统采用前后端分离架构，核心流程如下：

1. 用户在前端输入 `repoUrl` 和 `prNumber`
2. 后端校验参数并创建 Review 任务
3. `GitHubClient` 从 GitHub 获取 PR 基本信息和变更文件
4. Diff 解析层将 patch 转换为结构化审查上下文
5. `ReviewContextBuildService` 识别 PR 类型并构建 AI 输入上下文
6. `ModelClient` 调用 OpenAI 兼容模型接口
7. 后端解析模型输出，执行发现项后处理、去重、降级和兼容回填
8. 持久化 Review 结果、发现项、反馈记录和模型调用日志
9. 前端展示最终审查报告、发现项详情和反馈状态

## 仓库结构

```text
pr-review-agent/
├── backend/        # Spring Boot 后端
├── frontend/       # React + Vite 前端
├── docs/           # 项目文档
├── screenshots/    # 可选演示截图
├── README.md
├── AGENTS.md
└── .gitignore
```

## 快速开始

### 环境要求

请确保本地具备以下环境：

- JDK 21 或更高版本
- Maven 3.9 或更高版本
- Node.js 18 或更高版本
- npm 9 或更高版本
- MySQL 8.x（如果你要连接真实数据库）

### 1. 克隆仓库

```bash
git clone <你的仓库地址>
cd pr-review-agent
```

### 2. 启动后端

后端支持两种常见运行方式：

- 使用默认 H2 内存数据库快速启动
- 使用 MySQL 和真实外部配置进行本地开发

#### 使用 H2 快速启动

```bash
cd backend
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8080
```

健康检查：

```http
GET http://localhost:8080/api/health
```

预期响应：

```json
{
  "status": "UP"
}
```

#### 使用 MySQL 启动

先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS pr_review_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

然后在启动前设置环境变量：

```powershell
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/pr_review_agent?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
$env:JPA_DIALECT="org.hibernate.dialect.MySQLDialect"
$env:GITHUB_TOKEN="your_github_token"
$env:MODEL_API_BASE_URL="https://api.openai.com/v1"
$env:MODEL_API_KEY="your_model_api_key"
$env:MODEL_API_MODEL="gpt-4.1-mini"
$env:MODEL_API_TIMEOUT_SECONDS="60"

cd backend
mvn spring-boot:run
```

也可以参考本地示例配置文件：

```text
backend/src/main/resources/application-local.example.yml
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

开发环境下，Vite 会把 `/api` 请求代理到：

```text
http://localhost:8080
```

### 4. 验证完整流程

1. 启动后端
2. 启动前端
3. 打开 `http://localhost:5173`
4. 输入公开 GitHub 仓库地址
5. 输入 PR 编号
6. 提交分析任务
7. 查看首页加载状态、最近一次分析记录和示例报告预览
8. 查看生成的审查结果、发现项详情、修复示例和错误提示

推荐测试的仓库地址格式示例：

- `https://github.com/octocat/Hello-World`
- `https://github.com/octocat/Hello-World.git`
- `git@github.com:octocat/Hello-World.git`

## 配置说明

后端支持从 `application.yml`、环境变量和本地忽略配置文件读取配置。

### 后端环境变量

| 变量名 | 是否必需 | 说明 |
| --- | --- | --- |
| `DB_URL` | 否 | 数据库 JDBC URL，默认使用 H2 |
| `DB_USERNAME` | 否 | 数据库用户名 |
| `DB_PASSWORD` | 否 | 数据库密码 |
| `DB_DRIVER` | 否 | JDBC 驱动类名 |
| `JPA_DIALECT` | 否 | Hibernate 方言 |
| `GITHUB_API_BASE_URL` | 否 | GitHub API 基础地址 |
| `GITHUB_TOKEN` | 否 | 可选 GitHub Token，可降低限流影响 |
| `GITHUB_API_TIMEOUT_SECONDS` | 否 | GitHub API 超时时间（秒） |
| `GITHUB_API_VERSION` | 否 | GitHub API Version Header |
| `MODEL_API_BASE_URL` | 真实 AI 审查时必需 | OpenAI 兼容 API 基础地址 |
| `MODEL_API_KEY` | 真实 AI 审查时必需 | 模型提供方 API Key |
| `MODEL_API_MODEL` | 真实 AI 审查时必需 | 模型名称 |
| `MODEL_API_TIMEOUT_SECONDS` | 否 | 模型接口超时时间（秒） |

### 本地忽略配置

推荐使用以下本地文件保存敏感配置：

- `backend/src/main/resources/application-local.yml`
- `.env`
- 其他已被 `.gitignore` 覆盖的本地私有配置文件

不要提交 Token、API Key、密码或本地私有配置文件。

## 接口概览

### 健康检查

```http
GET /api/health
```

### 创建 Review 任务

```http
POST /api/reviews
Content-Type: application/json

{
  "repoUrl": "https://github.com/octocat/Hello-World",
  "prNumber": 1
}
```

成功响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "repoUrl": "https://github.com/octocat/Hello-World",
    "repoOwner": "octocat",
    "repoName": "Hello-World",
    "prNumber": 1,
    "prType": "DOCUMENTATION",
    "resultViewType": "DOCUMENTATION_FINDINGS",
    "status": "SUCCESS",
    "findingCount": 2,
    "summary": "本次 PR 主要优化了 README 文档格式和命令展示。",
    "overallConclusion": "本次变更属于文档类 PR，建议优先处理文档格式与可执行性问题。",
    "errorMessage": null,
    "createdAt": "2026-05-31T14:00:00",
    "updatedAt": "2026-05-31T14:00:10"
  }
}
```

### 查询 Review 任务详情

```http
GET /api/reviews/{id}
```

该接口会返回：

- 任务状态
- PR 元数据
- Review 结果
- 结构化发现项列表
- 任务创建与更新时间

关键字段说明：

- `prType`：PR 类型
- `resultViewType`：结果展示类型
- `findingCount`：发现项数量
- `findings`：结构化发现项列表

详情响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "status": "SUCCESS",
    "prType": "DOCUMENTATION",
    "resultViewType": "DOCUMENTATION_FINDINGS",
    "findingCount": 1,
    "errorMessage": null,
    "pullRequest": {
      "repoUrl": "https://github.com/octocat/Hello-World",
      "repoOwner": "octocat",
      "repoName": "Hello-World",
      "prNumber": 1,
      "title": "docs: improve setup guide",
      "description": "优化 README 中的命令说明与格式展示",
      "author": "octocat",
      "sourceBranch": "docs/readme-update",
      "targetBranch": "main",
      "state": "open",
      "changedFiles": 1,
      "additions": 12,
      "deletions": 2,
      "commits": 1,
      "prUrl": "https://github.com/octocat/Hello-World/pull/1"
    },
    "reviewResult": {
      "summary": "本次 PR 主要调整了 README 的命令展示方式和说明文本。",
      "changedModules": ["README.md"],
      "reviewSuggestions": ["建议统一使用 Markdown 代码块展示命令。"],
      "testSuggestions": ["检查 GitHub 页面渲染效果，并手动执行示例命令。"],
      "overallConclusion": "本次变更属于文档类 PR，建议优先处理文档格式与可执行性问题。"
    },
    "findings": [
      {
        "id": 101,
        "filePath": "README.md",
        "lineNumber": 42,
        "codeSnippet": "mkdir demoCreates a directory",
        "findingLevel": "LOW",
        "findingKind": "RISK",
        "findingCategory": "DOCUMENTATION_FORMAT",
        "title": "命令与说明文本粘连",
        "description": "命令和说明文字直接拼接在同一行，可能影响可读性和复制执行体验。",
        "suggestion": "建议使用 Markdown 代码块或空行分隔命令与说明。",
        "beforeExample": "mkdir demoCreates a directory",
        "afterExample": "mkdir demo\\n\\nCreates a directory",
        "suggestedPatch": null,
        "diffUrl": "https://github.com/octocat/Hello-World/pull/1/files#diff-...",
        "confidence": 0.88,
        "feedbackStatus": "USEFUL"
      }
    ],
    "createdAt": "2026-05-31T14:00:00",
    "updatedAt": "2026-05-31T14:00:10"
  }
}
```

### 查询 Review 任务列表

```http
GET /api/reviews
```

列表结果中的单条任务包含以下核心字段：

- `taskId`
- `repoUrl`
- `repoOwner`
- `repoName`
- `prNumber`
- `prType`
- `resultViewType`
- `status`
- `findingCount`
- `summary`
- `overallConclusion`
- `errorMessage`

### 提交发现项反馈

```http
POST /api/reviews/{taskId}/findings/{findingId}/feedback
Content-Type: application/json

{
  "feedbackType": "USEFUL",
  "comment": "这条建议对我有帮助"
}
```

`feedbackType` 支持以下取值：

- `USEFUL`
- `FALSE_POSITIVE`
- `IGNORED`
- `FIXED`

成功响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "findingId": 101,
    "feedbackType": "USEFUL",
    "comment": "这条建议对我有帮助",
    "createdAt": "2026-05-31T14:02:00"
  }
}
```

### GitHub PR 调试接口

```http
GET /api/github/pr?repoUrl=xxx&prNumber=1
```

### 参数错误示例

```http
POST /api/reviews
Content-Type: application/json

{
  "repoUrl": "https://gitlab.com/example/repo",
  "prNumber": 0
}
```

错误响应示例：

```json
{
  "code": 400,
  "message": "prNumber must be a positive number.; repoUrl must be a valid GitHub repository URL.",
  "data": null
}
```

## 数据库说明

核心表如下：

- `review_task`
- `pull_request_info`
- `review_result`
- `risk_item`
- `review_feedback`
- `model_call_log`

应用启动时会根据当前 JPA 配置自动创建或更新表结构。

其中：

- `review_task` 保存任务基本信息、PR 类型、状态和发现项数量
- `review_result` 保存 PR 总结、建议和总体结论
- `risk_item` 保留原表名，但语义已经升级为结构化发现项表
- `review_feedback` 保存发现项反馈闭环记录
- `model_call_log` 保存模型调用元数据

当前版本中，`risk_item` 表的主要字段已经从旧的 `risk*` 体系升级为新的 `finding*` 体系：

- `findingLevel`
- `findingKind`
- `findingCategory`
- `beforeExample`
- `afterExample`
- `suggestedPatch`
- `diffUrl`
- `feedbackStatus`

为兼容已有数据，后端启动时会自动检测旧 `risk_level` / `risk_type` 列是否存在；如果存在，且对应新字段为空，会自动回填到新的 `finding_*` 字段中。

## 测试方式

### 后端测试

```bash
cd backend
mvn test
```

### 前端构建校验

```bash
cd frontend
npm run build
```

### 手动验证清单

- 后端能够正常启动
- 前端能够正常启动
- `GET /api/health` 返回 `UP`
- 非法 `repoUrl` 或 `prNumber` 能返回清晰错误信息
- 合法的 GitHub HTTPS 地址、带 `.git` 的克隆地址可以通过前端校验
- GitHub API 错误能以统一格式返回
- 前端能够展示加载状态、最近一次分析记录、Review 结果和后端错误提示
- 文档类 PR 能返回 `prType=DOCUMENTATION`
- 文档类发现项默认不再使用过高等级
- 发现项可以提交反馈并在详情中回显 `feedbackStatus`
- 返回首页后再次点击“查看报告”时，反馈状态不会丢失
- 点击“展开详情”时页面不会自动向上跳动，按钮位置保持稳定

## 常见问题

### 后端能启动，但 AI 审查失败

请检查以下变量是否已经配置：

- `MODEL_API_BASE_URL`
- `MODEL_API_KEY`
- `MODEL_API_MODEL`

### GitHub 请求被限流

建议配置 `GITHUB_TOKEN`，以减少匿名请求的限流影响。

### 前端无法连接后端

请确认：

- 后端运行在 `http://localhost:8080`
- 前端运行在 `http://localhost:5173`
- Vite 代理配置未被修改

### Java 版本过低

如果你当前终端里的 `JAVA_HOME` 不是 JDK 21+，请先切换再执行 Maven 命令。

## 项目文档

`docs/` 目录中包含更多项目资料：

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

## 安全说明

不要提交以下内容：

- `.env`
- `application-local.yml`
- GitHub Token
- 模型 API Key
- 密码
- 本地 IDE 私有配置

所有敏感信息都应通过环境变量或本地忽略配置文件提供。

## 版本更新

### 2026-05 精准化 Review 更新

当前版本新增了以下能力：

- 支持 PR 类型识别：`DOCUMENTATION`、`CODE`、`CONFIG`、`TEST`、`DEPENDENCY`、`CICD`、`MIXED`
- 支持结构化发现项输出：`findingLevel`、`findingKind`、`findingCategory`
- 引入 `ADVISORY` 作为独立建议等级
- 支持 `beforeExample`、`afterExample`、`suggestedPatch`、`diffUrl`
- 支持发现项反馈闭环接口：`POST /api/reviews/{taskId}/findings/{findingId}/feedback`
- 前端结果页改为卡片式发现项展示，并支持展开详情与反馈操作

### 2026-05 前端产品化体验更新

本轮前端围绕首页和结果页做了产品化增强：

- 首页新增能力说明、示例 Review 报告预览和最近一次分析记录
- 表单补充 GitHub 仓库地址格式校验和分析耗时提示
- 分析过程改为三阶段加载态，减少“提交后无反馈”的感受
- 结果页强化 PR 类型、风险等级、发现项数量和总体结论展示
- 发现项展开详情时保持当前位置，避免页面自动滚动带来的视觉跳动
- 反馈提交后会同步更新首页重新打开的报告视图，避免状态回退

如果你是从旧版本升级而来，需要注意：

- API 返回字段已从 `riskItems` 切换为 `findings`
- 字段名已从 `riskLevel` / `riskType` 切换为 `findingLevel` / `findingCategory`
- 任务汇总字段已从 `riskCount` 切换为 `findingCount`
- 文档类 PR 会优先展示“文档问题列表”，而不是统一使用“风险代码列表”
