# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。用户输入仓库地址和 PR 编号后，系统将获取对应 PR 的变更内容，并结合 AI 模型输出结构化代码审查结果。

当前仓库按照模块化方式逐步开发。现阶段已完成项目基础结构、后端基础服务和前端基础页面，便于后续继续接入 GitHub PR 获取、Diff 解析和 AI Review 流程。

## 项目目标

系统最终将输出以下内容：

- PR 变更总结
- 风险代码识别
- Review 建议
- 测试建议
- 总体结论

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

目录说明：

- `backend/`：后端服务代码
- `frontend/`：前端页面代码
- `docs/`：需求、技术选型、架构和模块计划文档
- `screenshots/`：演示截图，可选

## 当前进度

已完成模块：

1. 项目基础结构模块
2. 后端基础服务模块
3. 前端基础页面模块

当前可用能力：

- 后端可通过 Spring Boot 启动
- 提供 `GET /api/health` 健康检查接口
- 前端可通过 Vite 启动
- 首页已提供 GitHub 仓库地址输入框
- 首页已提供 PR 编号输入框
- 首页已提供“开始分析”按钮

当前尚未完成：

- 数据库实体与仓库层
- GitHub PR 数据获取
- Diff 解析与上下文构造
- AI 模型调用与 AI Review 分析
- Review 任务接口
- 前端结果展示与接口联调

## 项目文档

`docs/` 目录下包含以下核心文档：

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

其中 `docs/模块拆分与开发计划.md` 是当前开发顺序和模块边界的主要依据。

## 本地运行

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

健康检查接口：

```text
GET http://localhost:8080/api/health
```

预期返回：

```json
{
  "status": "UP"
}
```

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：

```text
http://localhost:5173
```

### 运行后端测试

```bash
cd backend
mvn test
```

## 前端基础页面验收

打开 `http://localhost:5173` 后，确认页面满足以下条件：

- 能显示系统标题 `AI PR Review 助手`
- 包含 `GitHub 仓库地址` 输入框
- 包含 `PR 编号` 输入框
- 包含 `开始分析` 按钮

## 配置说明

当前阶段不要求额外环境变量即可运行前端基础页面和后端健康检查。

后续接入 GitHub API、数据库和模型服务时，请通过环境变量或本地忽略配置文件管理敏感信息，不要将以下内容提交到仓库：

- GitHub Token
- 模型 API Key
- `.env`
- `application-local.yml`

## 开发说明

- 后端代码必须放在 `backend/` 下
- 前端代码必须放在 `frontend/` 下
- 项目文档必须放在 `docs/` 下
- 当前开发需严格按模块顺序推进
- 每次只实现当前指定模块，不提前实现后续功能
