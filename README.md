# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。

用户输入 GitHub 仓库地址和 PR 编号后，系统会自动获取 PR 基本信息与改动文件，解析 diff 构建审查上下文，调用 OpenAI 兼容模型生成结构化代码审查结果，并输出以下内容：

- PR 变更总结
- 变更模块说明
- 风险项列表
- Review 建议
- 测试建议
- 总体结论

项目包含 Spring Boot 后端、React 前端、任务与结果持久化，以及完整的参数校验和统一异常处理能力，能够作为一个可运行、可演示、可继续迭代的完整前后端项目使用。

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

## 功能特性

- 支持提交公开 GitHub 仓库地址和 PR 编号，创建审查任务
- 支持从 GitHub REST API 获取 PR 标题、作者、分支、改动文件和 patch 内容
- 支持将 diff 解析为结构化审查上下文，而不是直接把原始 patch 全量传给模型
- 支持通过 OpenAI-compatible Chat Completion API 生成结构化 AI Review 报告
- 支持持久化保存审查任务、PR 元数据、Review 结果、风险项和模型调用日志
- 支持前端展示加载状态、PR 信息、风险项、建议项和错误信息
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
- OpenAI-compatible 模型 API

## 系统架构

系统采用前后端分离架构，核心流程如下：

1. 用户在前端输入 `repoUrl` 和 `prNumber`
2. 后端校验参数并创建 Review 任务
3. `GitHubClient` 从 GitHub 获取 PR 基本信息和改动文件
4. Diff 解析层将 patch 转换为结构化审查上下文
5. `ModelClient` 调用 OpenAI-compatible 模型接口
6. 后端解析模型输出，保存 Review 结果和风险项
7. 前端展示最终审查报告和错误提示

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

这是验证后端是否能启动的最快方式：

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
7. 查看生成的审查结果和错误提示

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
| `MODEL_API_BASE_URL` | 真实 AI 审查时必需 | OpenAI-compatible API 基础地址 |
| `MODEL_API_KEY` | 真实 AI 审查时必需 | 模型提供方 API Key |
| `MODEL_API_MODEL` | 真实 AI 审查时必需 | 模型名称 |
| `MODEL_API_TIMEOUT_SECONDS` | 否 | 模型接口超时时间（秒） |

### 本地忽略配置

推荐使用以下本地文件保存敏感配置：

- `backend/src/main/resources/application-local.yml`
- `.env`
- 其他已经被 `.gitignore` 覆盖的本地私有配置文件

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
    "status": "SUCCESS",
    "riskCount": 2,
    "summary": "本次 PR 主要更新了示例文档并调整了部分代码结构。",
    "overallConclusion": "整体风险较低，但建议补充边界场景验证。",
    "errorMessage": null
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
- 风险项
- 任务时间信息

### 查询 Review 任务列表

```http
GET /api/reviews
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
- `model_call_log`

应用启动时会根据当前 JPA 配置自动创建或更新表结构。

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
- GitHub API 错误能以统一格式返回
- 前端能展示加载状态、审查结果和后端错误提示

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
