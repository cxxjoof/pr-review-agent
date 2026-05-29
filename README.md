# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。用户输入仓库地址和 PR 编号后，系统会获取对应 PR 的变更内容，结合 AI 模型完成分析，并输出结构化的代码审查结果。

## 项目目标

系统计划输出以下内容：

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

- `backend/`：后端服务代码目录
- `frontend/`：前端页面代码目录
- `docs/`：项目需求、设计、计划等文档目录
- `screenshots/`：演示截图目录，可选使用

## 项目文档

`docs/` 目录下包含以下核心文档：

- `需求分析.md`
- `技术选型.md`
- `架构设计.md`
- `数据库设计.md`
- `模块拆分与开发计划.md`

其中 `docs/模块拆分与开发计划.md` 是当前开发阶段的主要路线文档。

## 当前进度

当前已完成以下模块：

1. 项目基础结构模块
2. 后端基础服务模块

当前后端已具备：

- Spring Boot 基础工程结构
- Maven 构建配置
- 启动类 `PrReviewApplication`
- 健康检查控制器 `HealthController`
- `GET /api/health` 接口
- 基础测试用例

当前尚未完成：

- 前端基础页面
- 数据库实体与仓库层
- GitHub PR 数据获取
- Diff 解析与上下文构造
- AI 模型调用与 AI Review 分析
- Review 任务接口与前端完整交互

## 后端运行方式

进入后端目录后启动服务：

```bash
cd backend
mvn spring-boot:run
```

默认启动地址：

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

运行测试：

```bash
cd backend
mvn test
```

## 开发说明

- 后端代码必须放在 `backend/` 下
- 前端代码必须放在 `frontend/` 下
- 项目文档必须放在 `docs/` 下
- 不要在仓库中提交 API Key、GitHub Token、密码或本地敏感配置
- 配置项应从 `application.yml`、环境变量或本地忽略配置文件中读取

## 后续开发顺序

根据项目计划，后续模块顺序为：

1. 项目基础结构
2. 后端基础服务
3. 前端基础页面
4. 数据库实体与仓库层
5. GitHub PR 数据获取
6. Diff 解析与上下文构造
7. AI 模型调用
8. AI Review 分析
9. Review 任务接口
10. 前端 Review 交互展示
11. 异常处理与参数校验
12. 文档与演示材料
