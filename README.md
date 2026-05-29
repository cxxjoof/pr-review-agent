# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。用户输入仓库地址和 PR 编号后，系统会获取 PR 变更内容，并逐步接入 AI 分析能力，输出结构化的代码审查结果。

当前仓库按照模块拆分计划逐步开发，现阶段已完成：

1. 项目基础结构
2. 后端基础服务
3. 前端基础页面
4. 数据库实体与 Repository
5. GitHub PR 数据获取
6. Diff 解析与上下文构造

## 当前能力

- 后端可通过 Spring Boot 正常启动
- 提供 `GET /api/health` 健康检查接口
- 已完成 `review_task`、`pull_request_info`、`review_result`、`risk_item`、`model_call_log` 五张核心表的 JPA 映射
- 已完成 GitHub API 配置与 `GitHubClient`
- 支持解析 GitHub 仓库地址中的 `owner` 和 `repo`
- 支持根据 `repoUrl` 和 `prNumber` 获取 PR 基本信息
- 支持获取 PR changed files 与每个文件的 `patch` 内容
- 支持将 PR 基本信息保存到 `review_task` 和 `pull_request_info`
- 提供第五模块临时验收接口 `GET /api/github/pr`
- 已完成 `DiffParseService`，支持解析 hunk、新增行、删除行、上下文行与行号
- 已完成文件变更类型识别与文件类别识别，支持 `BACKEND`、`FRONTEND`、`CONFIG`、`DATABASE`、`TEST` 等分类
- 已完成 `ReviewContextBuildService`，支持构造结构化 `ReviewContext` 与 AI 可消费的摘要上下文
- 支持对空 patch、二进制文件或过大文件 patch 缺失场景做兜底处理
- 前端基础首页已可启动，包含仓库地址、PR 编号输入框和开始分析按钮

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
- OpenAI-compatible Model API

## 目录结构

```text
pr-review-agent/
├── backend/
├── frontend/
├── docs/
├── README.md
├── AGENTS.md
└── .gitignore
```

## 后端运行

### 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.x（如需按真实数据库方式验收）

### 使用默认内存库启动

当前后端默认可以使用 H2 内存数据库启动，适合快速验证接口与 JPA 映射：

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/api/health
```

预期返回：

```json
{
  "status": "UP"
}
```

### 使用 MySQL 启动

如需按 MySQL 验收，可先创建数据库：

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

## GitHub PR 获取模块说明

第五模块新增了 GitHub PR 数据获取能力，后端通过 `WebClient` 调用 GitHub REST API。

当前支持：

- 解析 `https://github.com/{owner}/{repo}`
- 解析 `git@github.com:{owner}/{repo}.git`
- 获取 PR 标题、描述、作者、源分支、目标分支、状态
- 获取 changed files、additions、deletions、commits
- 获取每个 changed file 的 `patch`
- 支持公开仓库直接访问
- 支持通过配置 `GITHUB_TOKEN` 缓解 API 频率限制
- 调用失败时返回明确错误信息

### 相关配置

可通过环境变量配置 GitHub API：

```powershell
$env:GITHUB_TOKEN="your_github_token"
$env:GITHUB_API_BASE_URL="https://api.github.com"
$env:GITHUB_API_TIMEOUT_SECONDS="20"
$env:GITHUB_API_VERSION="2022-11-28"
```

说明：

- `GITHUB_TOKEN` 可选，建议配置
- `GITHUB_API_BASE_URL` 默认值为 `https://api.github.com`
- `GITHUB_API_TIMEOUT_SECONDS` 默认值为 `20`
- `GITHUB_API_VERSION` 默认值为 `2022-11-28`

### 临时验收接口

```text
GET /api/github/pr?repoUrl=xxx&prNumber=1
```

示例请求：

```text
GET http://localhost:8080/api/github/pr?repoUrl=https://github.com/cxxjoof/pr-review-agent&prNumber=1
```

PowerShell 示例：

```powershell
Invoke-RestMethod "http://localhost:8080/api/github/pr?repoUrl=https://github.com/cxxjoof/pr-review-agent&prNumber=1"
```

接口返回内容包含：

- 任务 ID
- 仓库地址、仓库拥有者、仓库名称
- PR 编号
- PR 标题、描述、作者
- 源分支、目标分支、状态
- 修改文件数、增删行数、提交数
- changed files 列表与 patch 内容

## Diff 解析与上下文构造模块说明

第六模块在第五模块获取到 GitHub PR 文件变更后，继续对每个文件的 `patch` 内容做结构化处理，而不是直接把原始 diff 文本传给后续 AI 模块。

当前新增的核心能力包括：

- `DiffParseService`：按文件解析 GitHub patch
- `ReviewContextBuildService`：聚合 PR 级别的 Review 上下文
- `ChangedFileContext`：保存单文件的结构化 diff 结果
- `DiffLineDTO`：保存单行 diff 的类型、旧行号、新行号和内容
- `ReviewContext`：保存 PR 级别摘要、模块信息、统计信息和 AI 输入上下文
- `FileChangeType`：区分 `ADDED`、`MODIFIED`、`REMOVED`、`RENAMED`、`COPIED`

### 当前解析行为

- 识别 hunk 头，例如 `@@ -10,3 +10,4 @@`
- 提取新增行、删除行和上下文行
- 为 diff 行保留对应旧行号和新行号
- 识别文件变更类型
- 根据路径和后缀识别文件类别
- 统计单文件 hunk 数、增删行数、上下文行数
- 汇总 PR 级别的变更规模与模块信息
- 生成面向 AI 的精简上下文摘要，避免直接传递混乱原始 patch

### 文件类别识别

当前内置的文件类别识别包括：

- `BACKEND`
- `FRONTEND`
- `CONFIG`
- `DATABASE`
- `TEST`
- `DOCUMENTATION`
- `BUILD`
- `OTHER`

### 空 patch 兜底

对于 GitHub API 未返回 patch 的文件，例如二进制文件、图片文件或超大文件，系统会：

- 标记 `patchAvailable=false`
- 记录兜底说明 `patchNotice`
- 保留文件路径、变更类型和统计信息
- 在构造 AI 上下文时明确提示该文件无 patch 内容

### 当前模块边界

第六模块只负责“解析”和“构造上下文”，当前还没有新增对外 API，也不会在这一阶段提前实现：

- AI 模型调用
- AI Review 结果生成
- Review 正式任务接口

## 数据库验收检查

启动成功后，可执行以下 SQL：

```sql
SHOW TABLES;
```

预期至少包含：

```text
review_task
pull_request_info
review_result
risk_item
model_call_log
```

第五模块接口调用成功后，可继续检查：

```sql
SELECT * FROM review_task ORDER BY id DESC;
SELECT * FROM pull_request_info ORDER BY id DESC;
```

## 测试

后端测试：

```bash
cd backend
mvn test
```

当前测试覆盖：

- 健康检查接口
- JPA Repository 持久化
- GitHub 仓库地址解析
- GitHub PR 获取接口成功场景
- GitHub PR 获取接口失败场景
- Diff patch 行级解析
- 文件类别识别与空 patch 兜底
- ReviewContext 聚合与 AI 上下文构造

## 前端运行

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

## 当前开发边界

当前尚未完成以下模块：

- AI 模型客户端
- AI Review 分析
- Review 任务正式接口
- 前端 Review 结果展示
- 参数校验与异常处理完善
- 完整文档与演示材料

当前重点是保证第六模块完成后，主分支仍然可启动、可测试，并能继续衔接后续模块。

## 配置与安全

不要提交以下内容到仓库：

- `.env`
- `application-local.yml`
- GitHub Token
- 模型 API Key
- 数据库密码

敏感信息请通过环境变量或本地忽略配置文件管理。

## 参考文档

`docs/` 目录包含项目开发所需的核心文档：

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

其中 `docs/模块拆分与开发计划.md` 是当前模块开发顺序的主要依据。
