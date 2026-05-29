# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。用户输入仓库地址和 PR 编号后，系统会获取 PR 变更内容，并结合 AI 模型输出结构化代码审查结果。

当前仓库按模块顺序逐步开发，现阶段已经完成：

1. 项目基础结构
2. 后端基础服务
3. 前端基础页面
4. 数据库实体与 Repository

## 当前能力

- 后端可通过 Spring Boot 正常启动
- 提供 `GET /api/health` 健康检查接口
- 已完成 `review_task`、`pull_request_info`、`review_result`、`risk_item`、`model_call_log` 5 张核心表的 JPA 实体映射
- 已完成对应 Repository 定义与仓储层测试
- 前端基础首页已可启动，包含仓库地址、PR 编号输入和开始分析按钮

## 技术栈

后端：

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring WebClient
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

## 目录结构

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

### 1. 环境要求

- JDK 21 或更高版本
- Maven 3.9+
- MySQL 8.x（如需按真实数据库方式验收）

### 2. 使用默认内存库启动

当前后端默认可使用 H2 内存库启动，适合快速验证健康检查和 JPA 映射是否可加载：

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

### 3. 使用 MySQL 启动

如果要按第四模块验收标准验证真实 MySQL 建表，请先准备数据库，例如：

```sql
CREATE DATABASE IF NOT EXISTS pr_review_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

然后在 PowerShell 中设置环境变量后启动：

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

## 数据库验收检查

启动成功后，可以执行以下 SQL 检查核心表：

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

## 后端测试

```bash
cd backend
mvn test
```

当前仓储层测试会验证：

- Repository 能正常装配
- 5 张核心表的实体可完成基础持久化
- 按 `task_id`、状态、风险等级等查询可正常执行

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

- GitHub PR 数据获取
- Diff 解析与上下文构造
- AI 模型调用
- AI Review 分析
- Review 任务接口
- 前端结果展示与联调
- 参数校验与异常处理完善
- 完整文档与演示材料

本仓库当前重点是保证第四模块完成后，主分支仍可启动、可测试、可继续衔接后续模块。

## 配置与安全

请不要提交以下内容到仓库：

- `.env`
- `application-local.yml`
- GitHub Token
- 模型 API Key
- 数据库密码

敏感信息请通过环境变量或本地忽略配置文件管理。

## 参考文档

`docs/` 目录下包含项目开发所需的核心文档：

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

其中 `docs/模块拆分与开发计划.md` 是当前模块开发顺序的主要依据。
