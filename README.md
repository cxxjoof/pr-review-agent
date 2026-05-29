# AI PR Review 助手

AI PR Review 助手是一个面向 GitHub Pull Request 场景的全栈 Web 应用。用户输入仓库地址和 PR 编号后，系统会自动获取对应 PR 的变更内容，结合 AI 模型完成分析，并输出结构化的代码审查结果。

## 项目目标

系统计划输出以下内容：

- PR 变更总结
- 风险代码识别
- Review 建议
- 测试建议
- 总体结论

本仓库按模块逐步开发，目前处于项目基础结构阶段。

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
|-- backend/
|-- frontend/
|-- docs/
|-- screenshots/
|-- README.md
|-- AGENTS.md
`-- .gitignore
```

目录说明：

- `backend/`：后端服务代码目录
- `frontend/`：前端页面代码目录
- `docs/`：项目需求、设计、计划等文档目录
- `screenshots/`：演示截图目录，可选使用

## 项目文档

`docs/` 目录下已整理的项目文档包括：

- `需求分析.md`
- `技术选型.md`
- `架构设计.md`
- `数据库设计.md`
- `模块拆分与开发计划.md`

其中 `docs/模块拆分与开发计划.md` 是当前开发阶段的主要路线文档。

## 当前进度

当前已完成第一模块“项目基础结构”：

- 已创建基础目录结构
- 已整理项目参考文档到 `docs/`
- 已添加 `.gitignore` 基础忽略规则
- 暂未开始后端和前端的可运行代码实现

## 开发说明

- 后端代码必须放在 `backend/` 下
- 前端代码必须放在 `frontend/` 下
- 项目文档必须放在 `docs/` 下
- 不要在仓库中提交 API Key、GitHub Token、密码或本地敏感配置

## 后续开发顺序

根据项目计划，后续模块顺序为：

1. 项目基础结构
2. 后端基础服务
3. 前端基础页面
4. 数据库实体与仓库层
5. GitHub PR 数据获取
6. Diff 解析与上下文构建
7. AI 模型客户端
8. AI Review 分析
9. Review 任务接口
10. 前端 Review 流程与结果展示
11. 参数校验与异常处理
12. 文档与演示材料
