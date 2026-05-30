# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。用户输入仓库地址和 PR 编号后，系统会获取 PR 变更、构建审查上下文、调用 OpenAI 兼容模型生成结构化 Review 报告，并将任务、PR 信息、分析结果和风险项保存到数据库。

项目按照 [docs/模块拆分与开发计划.md](/C:/pr-review-agent/docs/模块拆分与开发计划.md) 逐步推进，当前已经完成到第十模块“前端 Review 工作流与结果展示”。

## 当前完成模块

1. 项目结构
2. 后端基础服务
3. 前端基础页面
4. 数据库实体与仓储
5. GitHub PR 数据获取
6. Diff 解析与 ReviewContext 构建
7. AI 模型客户端
8. AI Review 分析
9. Review 任务接口
10. 前端 Review 工作流与结果展示

## 当前能力

- 后端可提供 `GET /api/health`、`POST /api/reviews`、`GET /api/reviews/{id}`、`GET /api/reviews`
- GitHub PR 信息、改动文件、patch/diff 可被拉取并结构化处理
- AI Review 结果会落库到 `review_task`、`pull_request_info`、`review_result`、`risk_item`、`model_call_log`
- 前端已具备完整分析链路：
  - 输入 GitHub 仓库地址和 PR 编号
  - 提交分析请求
  - 展示加载状态
  - 展示 PR 基本信息、变更总结、风险项、Review 建议、测试建议和总体评价
  - 展示错误提示
- 当前模型提示词已强制要求中文输出，前端风险等级与风险类型也以中文展示

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

## 后端启动

### 环境要求

- JDK 21 或更高版本
- Maven 3.9+
- 如需验证真实数据库，建议使用 MySQL 8.x

### 默认启动方式

后端默认使用 H2 内存数据库，可以快速本地验证：

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

### 使用本地忽略配置启动

如果你在本地准备了 `backend/src/main/resources/application-local.yml`，建议启用 `local` profile：

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
cd backend
mvn spring-boot:run
```

如果机器上的默认 `java` 不是 21+，需要先切换 `JAVA_HOME`。例如在 PowerShell 中：

```powershell
$env:JAVA_HOME="C:\Path\To\JDK21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE="local"
cd backend
mvn spring-boot:run
```

### 使用 MySQL 启动

如需接入真实数据库，可先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS pr_review_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

然后通过环境变量或本地忽略配置文件提供以下信息：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DRIVER`
- `JPA_DIALECT`
- `GITHUB_TOKEN`（可选，公开仓库不是必须）
- `MODEL_API_BASE_URL`
- `MODEL_API_KEY`
- `MODEL_API_MODEL`
- `MODEL_API_TIMEOUT_SECONDS`（可选）

示例：

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

可参考本地示例文件：

```text
backend/src/main/resources/application-local.example.yml
```

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

说明：

- 前端通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`
- 启动前端前，请先确保后端已经可用

## 第十模块联调方式

前端当前已接入完整 Review 工作流，推荐按下面步骤验证：

1. 启动后端
2. 启动前端
3. 打开 `http://localhost:5173`
4. 输入公开 GitHub 仓库地址和 PR 编号
5. 点击“开始分析”
6. 检查加载态、结果页和错误提示

推荐使用已验证可用的公开 PR：

```text
仓库地址：https://github.com/octocat/Hello-World
PR 编号：1
```

## Review API

### 1. 创建 Review 任务

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
    "riskCount": 4,
    "summary": "该 PR 修改了 README 文件，并新增了若干 Git 初始化示例。",
    "overallConclusion": "当前变更以文档更新为主，建议优化排版和可读性。",
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

### 4. GitHub PR 临时验收接口

```text
GET /api/github/pr?repoUrl=xxx&prNumber=1
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

重点接口测试：

```bash
cd backend
mvn test -Dtest=ReviewControllerTest
```

## 当前边界

当前尚未完成的模块：

- 第十一模块：参数校验与统一异常处理完善
- 第十二模块：最终文档与演示材料

当前实际情况：

- 前端端到端分析页面：已完成
- 后端 AI Review 能力：已完成
- 统一异常返回和错误语义优化：待第十一模块继续完善
- 最终演示材料、截图和完整交付文档：待第十二模块完善

## 安全说明

不要提交以下文件或敏感信息：

- `.env`
- `application-local.yml`
- GitHub Token
- 模型 API Key
- 数据库密码
- IDE 本地私有配置

敏感信息请通过环境变量或本地忽略配置文件提供。当前 `.gitignore` 已覆盖常见本地配置、日志、构建产物和密钥文件。

## 参考文档

开发前请优先阅读 `docs/` 目录下的文档：

- [需求分析.md](/C:/pr-review-agent/docs/需求分析.md)
- [技术选型.md](/C:/pr-review-agent/docs/技术选型.md)
- [架构设计.md](/C:/pr-review-agent/docs/架构设计.md)
- [数据库设计.md](/C:/pr-review-agent/docs/数据库设计.md)
- [模块拆分与开发计划.md](/C:/pr-review-agent/docs/模块拆分与开发计划.md)

其中 `docs/模块拆分与开发计划.md` 是主开发路线图。
