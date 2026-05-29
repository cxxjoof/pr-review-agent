# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。
用户输入仓库地址和 PR 编号后，系统会获取 PR 变更、构建审查上下文，并逐步接入 AI 分析能力，生成结构化的代码审查结果。

项目严格按照模块顺序开发。当前已完成的模块包括：

1. 项目结构
2. 后端基础服务
3. 前端基础页面
4. 数据库实体与仓储
5. GitHub PR 数据获取
6. Diff 解析与 ReviewContext 构建
7. AI 模型客户端
8. AI Review 分析

## 当前能力

- 后端 Spring Boot 服务可以正常启动
- 提供健康检查接口 `GET /api/health`
- 已完成 `review_task`、`pull_request_info`、`review_result`、`risk_item`、`model_call_log` 的 JPA 映射
- `GitHubClient` 可以解析仓库地址并获取 PR 基本信息和改动文件
- `DiffParseService` 与 `ReviewContextBuildService` 可以将原始 patch 转换为适合 AI 分析的结构化上下文
- `ModelClient` 可以调用 OpenAI 兼容的 `chat/completions` 接口
- 模型 API 地址、API Key、模型名称和超时时间均可配置
- 模型调用元数据会写入 `model_call_log`
- 模型调用失败时，会将鉴权失败、限流、超时、响应格式异常等情况转换为明确的后端错误
- `AiReviewService` 已具备基于真实 PR 上下文生成结构化 AI Review 报告的能力
- AI Review 输出已支持：PR 总结、变更模块、风险项、Review 建议、测试建议、总体结论
- AI Review 结果会保存到 `review_result`，风险项会保存到 `risk_item`
- AI Review 执行过程中会更新 `review_task` 的状态和 `risk_count`
- 当模型返回的 JSON 不可解析时，后端会生成兜底报告，而不是静默失败

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
- 如果要验证真实数据库，建议使用 MySQL 8.x

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

cd backend
mvn spring-boot:run
```

也可以参考本地示例配置文件：

```text
backend/src/main/resources/application-local.example.yml
```

## 第八模块：AI Review 分析

第八模块基于前面已经完成的 GitHub 获取、Diff 解析、ReviewContext 构建和模型客户端能力，补齐了第一版正式的后端 AI Review 分析流程。

### 第八模块新增内容

- `AiReviewService`：串联 AI Review 分析主流程
- `PromptBuildService`：基于 `ReviewContext` 构建结构化 Prompt
- `dto/review/`：解析 AI Review 报告和风险项
- `ReviewReportService`：保存 `review_result`
- `RiskItemService`：保存 `risk_item`
- `JsonParseUtils`：从模型响应中提取 JSON，兼容 fenced JSON 代码块
- 模型返回格式异常时的兜底处理
- 覆盖以下场景的测试：
  - Prompt 构建
  - AI Review 成功路径
  - 无效 JSON 的兜底路径
  - 模型调用失败路径

### AI Review 输出结构

后端当前要求模型返回结构化 JSON，主要字段包括：

- `summary`
- `changedModules`
- `riskItems`
- `reviewSuggestions`
- `testSuggestions`
- `overallConclusion`

其中每个 `riskItems` 条目支持：

- `filePath`
- `lineNumber`
- `codeSnippet`
- `riskLevel`
- `riskType`
- `description`
- `suggestion`
- `confidence`

### 当前模块边界

第八模块只补齐了后端 AI Review 分析能力，目前还没有实现：

- `POST /api/reviews`
- `GET /api/reviews/{id}`
- 前端结果展示页面
- 前后端完整 Review 工作流

也就是说，AI Review 能力现在已经存在于后端 Service 层，但正式的 Review 任务 API 和 Web 端到端流程仍然属于第九、第十模块。

## 第七模块：AI 模型客户端

第七模块提供的是模型调用基础能力，本身不负责生成正式 AI Review 报告，也不包含最终的 Review 任务接口。

### 第七模块新增内容

- `ModelProperties`：模型配置项
- `ModelConfig`：模型 `WebClient` 配置
- `dto/model/`：模型请求与响应 DTO
- `ModelClient`：OpenAI 兼容聊天接口调用
- `ModelCallLogService`：模型调用日志落库
- 成功与失败路径的单元测试

### 支持的模型配置

当前后端支持以下环境变量：

```powershell
$env:MODEL_API_BASE_URL="https://api.openai.com/v1"
$env:MODEL_API_KEY="your_model_api_key"
$env:MODEL_API_MODEL="gpt-4.1-mini"
$env:MODEL_API_TIMEOUT_SECONDS="60"
```

对应配置形式如下：

```yaml
model:
  api:
    base-url: ${MODEL_API_BASE_URL:https://api.openai.com/v1}
    api-key: ${MODEL_API_KEY:}
    model-name: ${MODEL_API_MODEL:}
    timeout-seconds: ${MODEL_API_TIMEOUT_SECONDS:60}
```

### 当前模型客户端行为

- 使用 `POST /chat/completions`
- 支持 OpenAI 兼容的请求和响应格式
- 拒绝空消息请求
- 拒绝没有有效 assistant 内容的响应
- 成功或失败都会记录到 `model_call_log`
- 对以下失败场景返回明确错误：
  - API Key 无效
  - 触发限流
  - 请求超时
  - 上游响应格式异常
  - 通用上游调用失败

## GitHub PR 获取

当前后端为 GitHub PR 获取模块提供了一个临时验收接口：

```text
GET /api/github/pr?repoUrl=xxx&prNumber=1
```

示例：

```text
GET http://localhost:8080/api/github/pr?repoUrl=https://github.com/cxxjoof/pr-review-agent&prNumber=1
```

PowerShell 示例：

```powershell
Invoke-RestMethod "http://localhost:8080/api/github/pr?repoUrl=https://github.com/cxxjoof/pr-review-agent&prNumber=1"
```

## 数据库验证

启动后可以检查核心表是否已创建：

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

其中：

- 第七模块重点验证 `model_call_log`
- 第八模块重点验证：
  - `review_result`
  - `risk_item`
  - `review_task.status`
  - `review_task.risk_count`

## 测试

后端测试：

```bash
cd backend
mvn test
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

- Review 任务接口
- 前端 Review 结果展示
- 参数校验与异常处理完善
- 最终文档与演示材料

## 真实联调说明

完成第八模块后，后端已经具备真实模型联调能力，但还不能通过 Web 页面直接完成端到端分析。

当前实际情况：

- 后端 AI Review 能力：已具备
- 正式 Review API：尚未实现
- 前端端到端分析页面：尚未实现

如果现在要接入真实模型做联调，下一步建议先完成第九模块 Review API，这样 AI Review 流程才能通过稳定接口触发。

## 安全说明

不要提交以下文件或敏感信息：

- `.env`
- `application-local.yml`
- GitHub Token
- 模型 API Key
- 数据库密码
- IDE 本地敏感配置

敏感信息请通过环境变量或本地忽略配置文件提供。

## 参考文档

开发前请优先阅读 `docs/` 目录下的文档：

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

其中 `docs/模块拆分与开发计划.md` 是当前项目的主开发路线图。
