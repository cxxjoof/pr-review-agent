# AI PR Review Assistant

AI PR Review Assistant is a full-stack web application for GitHub Pull Request review scenarios.
Users provide a repository URL and PR number, and the system fetches PR changes, prepares review context, and progressively integrates AI analysis to generate structured review results.

The repository is developed strictly by module. The current completed modules are:

1. Project structure
2. Backend base service
3. Frontend base page
4. Database entities and repositories
5. GitHub PR data fetching
6. Diff parsing and review context building
7. AI model client

## Current Capabilities

- The backend can start with Spring Boot.
- `GET /api/health` is available for health checks.
- JPA mappings for `review_task`, `pull_request_info`, `review_result`, `risk_item`, and `model_call_log` are in place.
- `GitHubClient` can parse repository URLs and fetch PR metadata and changed files.
- `DiffParseService` and `ReviewContextBuildService` can convert raw PR patches into structured AI-ready review context.
- `ModelClient` can call an OpenAI-compatible `chat/completions` API.
- Model API base URL, API key, model name, and timeout are configurable.
- Model call metadata is persisted into `model_call_log`.
- Model-side failures such as unauthorized access, rate limits, timeout, and invalid responses are mapped to clear backend errors.

## Tech Stack

Backend:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring WebFlux `WebClient`
- Spring Data JPA
- MySQL
- Maven

Frontend:

- React
- Vite
- Ant Design
- Axios

External services:

- GitHub REST API
- OpenAI-compatible model API

## Repository Structure

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

## Backend Run

### Requirements

- JDK 21
- Maven 3.9+
- MySQL 8.x if you want to verify with a real database

### Start with the default in-memory database

The backend can start with H2 by default for quick verification:

```bash
cd backend
mvn spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP"
}
```

### Start with MySQL

Create the database first if needed:

```sql
CREATE DATABASE IF NOT EXISTS pr_review_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

Then set environment variables in PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/pr_review_agent?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
$env:JPA_DIALECT="org.hibernate.dialect.MySQLDialect"

cd backend
mvn spring-boot:run
```

You can also use the local example config:

```text
backend/src/main/resources/application-local.example.yml
```

## Module 7: AI Model Client

Module 7 adds the model access layer only. It does not yet generate formal AI review reports and does not add the final review task APIs.

### What was added

- `ModelProperties` for model configuration
- `ModelConfig` for model `WebClient`
- `dto/model/` request and response DTOs
- `ModelClient` for OpenAI-compatible chat completion calls
- `ModelCallLogService` for persisting model call metadata
- Unit tests for success and failure paths

### Supported model configuration

The backend now supports these environment variables:

```powershell
$env:MODEL_API_BASE_URL="https://api.openai.com/v1"
$env:MODEL_API_KEY="your_model_api_key"
$env:MODEL_API_MODEL="gpt-4.1-mini"
$env:MODEL_API_TIMEOUT_SECONDS="60"
```

Config file equivalent:

```yaml
model:
  api:
    base-url: ${MODEL_API_BASE_URL:https://api.openai.com/v1}
    api-key: ${MODEL_API_KEY:}
    model-name: ${MODEL_API_MODEL:}
    timeout-seconds: ${MODEL_API_TIMEOUT_SECONDS:60}
```

### Current model behavior

- Uses `POST /chat/completions`
- Supports OpenAI-compatible request and response format
- Rejects empty request messages
- Rejects responses without usable assistant content
- Records success or failure into `model_call_log`
- Returns clear backend errors for:
  - invalid API key
  - rate limit
  - timeout
  - invalid upstream response
  - generic upstream failure

### Current module boundary

Module 7 only provides the base model calling capability. It does not yet implement:

- AI review result generation
- risk item extraction
- review suggestion generation
- test suggestion generation
- formal review APIs such as `POST /api/reviews`

## GitHub PR Fetching

The backend currently provides a temporary acceptance API for the GitHub PR fetching module:

```text
GET /api/github/pr?repoUrl=xxx&prNumber=1
```

Example:

```text
GET http://localhost:8080/api/github/pr?repoUrl=https://github.com/cxxjoof/pr-review-agent&prNumber=1
```

PowerShell example:

```powershell
Invoke-RestMethod "http://localhost:8080/api/github/pr?repoUrl=https://github.com/cxxjoof/pr-review-agent&prNumber=1"
```

## Database Verification

After startup, you can check the core tables:

```sql
SHOW TABLES;
```

Expected tables:

```text
review_task
pull_request_info
review_result
risk_item
model_call_log
```

For module 7, `model_call_log` is the key verification point.

## Test

Backend tests:

```bash
cd backend
mvn test
```

Current coverage includes:

- health controller
- repository persistence
- GitHub repository URL parsing
- GitHub PR fetch flow
- diff parsing
- review context aggregation
- model client success and failure paths
- model call log persistence behavior

Frontend run:

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

## Current Development Boundary

The following modules are not completed yet:

- AI Review analysis
- Review task APIs
- frontend review result display
- validation and exception handling completion
- final documentation and demo materials

## Security

Do not commit the following files or values:

- `.env`
- `application-local.yml`
- GitHub tokens
- model API keys
- database passwords
- IDE local secrets

Use environment variables or ignored local config files for sensitive values.

## Reference Documents

Please check the documents in `docs/` before implementing any module:

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

`docs/模块拆分与开发计划.md` is the main development roadmap.
