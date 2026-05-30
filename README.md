# AI PR Review Assistant

AI PR Review Assistant is a full-stack web application for reviewing GitHub Pull Requests with an AI model.

Users provide a GitHub repository URL and a pull request number. The system fetches PR metadata and changed files from GitHub, parses the diff into structured review context, calls an OpenAI-compatible model, and generates:

- PR summary
- changed modules
- risk items
- review suggestions
- test suggestions
- overall conclusion

The project includes a Spring Boot backend, a React frontend, persistent review records, and unified validation and exception handling for the end-to-end review flow.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Overview](#api-overview)
- [Database](#database)
- [Testing](#testing)
- [Common Issues](#common-issues)
- [Documentation](#documentation)

## Features

- Submit a review task with a public GitHub repository URL and PR number
- Fetch PR title, author, branch information, changed files, and patch content from GitHub REST API
- Parse diffs into structured review context instead of sending raw patches directly to the model
- Generate structured AI review reports through an OpenAI-compatible chat completion API
- Persist review tasks, PR metadata, AI review results, risk items, and model call logs
- Display loading states, review summaries, risk items, suggestions, and error messages in the frontend
- Return unified backend responses and clear error messages for validation errors, upstream failures, and timeout scenarios

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring WebFlux `WebClient`
- Spring Data JPA
- Maven
- MySQL
- H2 for local quick start

### Frontend

- React
- Vite
- Ant Design
- Axios

### External Services

- GitHub REST API
- OpenAI-compatible model API

## Architecture

The application follows a standard frontend-backend separation:

1. The user submits `repoUrl` and `prNumber` from the React UI.
2. The Spring Boot backend validates the request and creates a review task.
3. `GitHubClient` fetches PR metadata and changed files from GitHub.
4. The diff parsing layer converts raw patch data into structured review context.
5. `ModelClient` sends the prompt to an OpenAI-compatible model API.
6. The backend parses the model output and stores the review result and risk items.
7. The frontend displays the final report and any task errors in a readable format.

## Repository Structure

```text
pr-review-agent/
├── backend/        # Spring Boot backend
├── frontend/       # React + Vite frontend
├── docs/           # project documents
├── screenshots/    # optional demo screenshots
├── README.md
├── AGENTS.md
└── .gitignore
```

## Getting Started

### Prerequisites

Make sure the following tools are available in your environment:

- JDK 21 or later
- Maven 3.9 or later
- Node.js 18 or later
- npm 9 or later
- MySQL 8.x if you want to run with a real database

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd pr-review-agent
```

### 2. Start the Backend

The backend can run in two common modes:

- Quick start with the default in-memory H2 database
- Local development with MySQL and external API credentials

#### Quick Start with H2

This mode is the fastest way to verify that the backend can start.

```bash
cd backend
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

Health check:

```http
GET http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP"
}
```

#### Run with MySQL

Create a local database first:

```sql
CREATE DATABASE IF NOT EXISTS pr_review_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

Then provide the required environment variables before starting the backend:

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

You can also copy the example local config and maintain your own ignored local file:

```text
backend/src/main/resources/application-local.example.yml
```

### 3. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

The Vite dev server proxies `/api` requests to:

```text
http://localhost:8080
```

### 4. Try the Full Review Flow

1. Start the backend
2. Start the frontend
3. Open `http://localhost:5173`
4. Enter a public GitHub repository URL
5. Enter a PR number
6. Submit the review task
7. Check the generated review report and any error prompts

## Configuration

The backend reads configuration from `application.yml`, environment variables, or local ignored files.

### Backend Environment Variables

| Variable | Required | Description |
| --- | --- | --- |
| `DB_URL` | No | Database JDBC URL. Defaults to H2 in-memory |
| `DB_USERNAME` | No | Database username |
| `DB_PASSWORD` | No | Database password |
| `DB_DRIVER` | No | JDBC driver class |
| `JPA_DIALECT` | No | Hibernate dialect |
| `GITHUB_API_BASE_URL` | No | GitHub API base URL |
| `GITHUB_TOKEN` | No | Optional GitHub token. Helps with rate limits |
| `GITHUB_API_TIMEOUT_SECONDS` | No | GitHub API timeout in seconds |
| `GITHUB_API_VERSION` | No | GitHub API version header |
| `MODEL_API_BASE_URL` | Yes for real AI review | OpenAI-compatible API base URL |
| `MODEL_API_KEY` | Yes for real AI review | Model provider API key |
| `MODEL_API_MODEL` | Yes for real AI review | Model name |
| `MODEL_API_TIMEOUT_SECONDS` | No | Model API timeout in seconds |

### Local Ignored Configuration

Recommended local files:

- `backend/src/main/resources/application-local.yml`
- `.env`
- any local-only secret file already covered by `.gitignore`

Do not commit tokens, API keys, passwords, or local secret config files.

## API Overview

### Health Check

```http
GET /api/health
```

### Create Review Task

```http
POST /api/reviews
Content-Type: application/json

{
  "repoUrl": "https://github.com/octocat/Hello-World",
  "prNumber": 1
}
```

Example success response:

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

### Get Review Task Detail

```http
GET /api/reviews/{id}
```

This endpoint returns:

- task status
- PR metadata
- review result
- risk items
- task timestamps

### List Review Tasks

```http
GET /api/reviews
```

### Temporary GitHub Fetch Endpoint

```http
GET /api/github/pr?repoUrl=xxx&prNumber=1
```

### Validation Error Example

```http
POST /api/reviews
Content-Type: application/json

{
  "repoUrl": "https://gitlab.com/example/repo",
  "prNumber": 0
}
```

Example error response:

```json
{
  "code": 400,
  "message": "prNumber must be a positive number.; repoUrl must be a valid GitHub repository URL.",
  "data": null
}
```

## Database

Core tables:

- `review_task`
- `pull_request_info`
- `review_result`
- `risk_item`
- `model_call_log`

The backend automatically creates or updates tables based on JPA configuration when the application starts.

## Testing

### Backend Tests

```bash
cd backend
mvn test
```

### Frontend Build Check

```bash
cd frontend
npm run build
```

### Manual Verification Checklist

- backend can start successfully
- frontend can start successfully
- `GET /api/health` returns `UP`
- invalid `repoUrl` or `prNumber` returns a clear validation error
- GitHub API errors are returned in unified response format
- frontend can display loading states, review results, and backend error messages

## Common Issues

### Backend starts but AI review fails

Check whether these variables are configured:

- `MODEL_API_BASE_URL`
- `MODEL_API_KEY`
- `MODEL_API_MODEL`

### GitHub requests are rate limited

Provide a `GITHUB_TOKEN` to reduce unauthenticated rate-limit problems.

### Frontend cannot reach the backend

Make sure:

- backend is running on `http://localhost:8080`
- frontend is running on `http://localhost:5173`
- Vite proxy configuration is unchanged

### Java version is too low

If your shell is not using JDK 21+, switch `JAVA_HOME` before running Maven.

## Documentation

Additional project documents are available in the `docs/` directory:

- `docs/需求分析.md`
- `docs/技术选型.md`
- `docs/架构设计.md`
- `docs/数据库设计.md`
- `docs/模块拆分与开发计划.md`

## Security Notes

Never commit:

- `.env`
- `application-local.yml`
- GitHub tokens
- model provider API keys
- passwords
- local IDE secret files

Use environment variables or ignored local configuration files for all secrets.
