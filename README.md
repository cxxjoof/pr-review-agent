# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。用户输入仓库地址和 PR 编号后，系统会获取 PR 变更、构建审查上下文、调用 OpenAI 兼容模型生成结构化 Review 报告，并将任务、PR 信息、分析结果和风险项保存到数据库。

项目严格按照 `docs/模块拆分与开发计划.md` 的顺序推进。当前已完成模块：

1. 项目结构
2. 后端基础服务
3. 前端基础页面
4. 数据库实体与仓储
5. GitHub PR 数据获取
6. Diff 解析与 ReviewContext 构建
7. AI 模型客户端
8. AI Review 分析
9. Review 任务接口

## 当前能力

- 后端 Spring Boot 服务可正常启动
- 提供健康检查接口 `GET /api/health`
- 已完成 `review_task`、`pull_request_info`、`review_result`、`risk_item`、`model_call_log` 的 JPA 映射
- `GitHubClient` 可以解析 GitHub 仓库地址并获取 PR 基本信息和改动文件
- `DiffParseService` 与 `ReviewContextBuildService` 可以将原始 patch 转换为适合 AI 分析的结构化上下文
- `ModelClient` 可以调用 OpenAI 兼容的 `chat/completions` 接口，并记录 `model_call_log`
- `AiReviewService` 可以基于真实 PR 上下文生成结构化 AI Review 报告，并在模型返回非标准 JSON 时生成兜底报告
- 已提供正式 Review 任务接口：
  - `POST /api/reviews`
  - `GET /api/reviews/{id}`
  - `GET /api/reviews`
- 成功响应统一为 `code / message / data` 结构
- Review 任务执行完成后会更新 `review_task.status`、`review_task.risk_count`，并落库 `review_result` 与 `risk_item`

## 技术栈

后端：

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring WebFlux `WebClient`
- Spring Data JPA
- MySQL
- Maven

前端：

- React
- Vite
- Ant Design
- Axios

外部服务：

- GitHub REST API
- OpenAI 兼容模型 API

## 仓库结构

```text
pr-review-agent/
├── backend/
├── frontend/
├── docs/
├── screenshots/
├── README.md
├── AGENTS.md
└── .gitignore
```

## 后端运行

### 环境要求

- JDK 21
- Maven 3.9+
- 如需验证真实数据库，建议使用 MySQL 8.x

### 使用默认内存数据库启动

为了方便快速验证，后端默认可使用 H2 启动：

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/api/health
```

预期响应：

```json
{
  "status": "UP"
}
```

### 使用 MySQL 启动

如需使用真实数据库，可先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS pr_review_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

然后在 PowerShell 中设置环境变量：

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

## Review API

第九模块已经提供正式 Review 任务接口，用于串联 GitHub 获取、diff 解析、AI 分析和结果查询。

### 1. 创建 Review 任务

```http
POST /api/reviews
Content-Type: application/json

{
  "repoUrl": "https://github.com/cxxjoof/pr-review-agent",
  "prNumber": 1
}
```

示例响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "repoUrl": "https://github.com/cxxjoof/pr-review-agent",
    "repoOwner": "cxxjoof",
    "repoName": "pr-review-agent",
    "prNumber": 1,
    "status": "SUCCESS",
    "riskCount": 2,
    "summary": "本次 PR 主要补充了 Review 任务接口。",
    "overallConclusion": "存在中等风险，建议重点关注异常处理和联调结果。",
    "errorMessage": null
  }
}
```

### 2. 查询任务详情

```http
GET /api/reviews/{id}
```

该接口会返回：

- 任务状态
- PR 基本信息
- Review 结果
- 风险项列表

### 3. 查询任务列表

```http
GET /api/reviews
```

该接口会按创建时间倒序返回历史任务摘要，方便前端后续接入任务历史列表。

## GitHub PR 临时验收接口

GitHub PR 获取模块之前保留了一个临时验收接口，目前仍可用于单独验证 GitHub 接入：

```text
GET /api/github/pr?repoUrl=xxx&prNumber=1
```

示例：

```powershell
Invoke-RestMethod "http://localhost:8080/api/github/pr?repoUrl=https://github.com/cxxjoof/pr-review-agent&prNumber=1"
```

## 数据库验证

启动后可检查核心表是否已创建：

```sql
SHOW TABLES;
```

预期表：

```text
review_task
pull_request_info
review_result
risk_item
model_call_log
```

重点验证项：

- `review_task.status`
- `review_task.risk_count`
- `pull_request_info`
- `review_result`
- `risk_item`
- `model_call_log`

## 测试

后端全量测试：

```bash
cd backend
mvn test
```

第九模块重点测试：

```bash
cd backend
mvn test -Dtest=ReviewControllerTest
```

当前测试覆盖包括：

- 健康检查接口
- Repository 持久化
- GitHub 仓库地址解析
- GitHub PR 获取流程
- Diff 解析
- ReviewContext 聚合
- 模型客户端成功与失败路径
- 模型调用日志落库
- AI Review Prompt 构建
- AI Review 成功路径
- AI Review 无效 JSON 兜底路径
- AI Review 失败路径及任务状态更新
- Review 任务创建、详情查询、历史列表接口

前端运行：

```bash
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

## 当前开发边界

以下模块尚未完成：

- 第十模块：前端 Review 工作流与结果展示
- 第十一模块：参数校验与统一异常处理完善
- 第十二模块：最终文档与演示材料

当前实际情况：

- 后端 AI Review 能力：已具备
- 正式 Review API：已具备
- 前端端到端分析页面：尚未完成
- 错误响应统一收口：待第十一模块完善

## 安全说明

不要提交以下文件或敏感信息：

- `.env`
- `application-local.yml`
- GitHub Token
- 模型 API Key
- 数据库密码
- IDE 本地私有配置

敏感信息请通过环境变量或本地忽略配置文件提供。

## 参考文档

开发前请优先阅读 `docs/` 目录下的文档：

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

其中 `docs/模块拆分与开发计划.md` 是当前项目的主开发路线图。
