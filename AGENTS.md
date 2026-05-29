# AGENTS.md

## Project Overview

This project is an **AI PR Review Assistant**.

Users provide a GitHub repository URL and Pull Request number. The system fetches PR changes from GitHub, analyzes the diff with an AI model, and generates:

* PR change summary
* risky code detection
* review suggestions
* test suggestions
* overall conclusion

This project should be developed as a runnable full-stack web application, not just an AI API demo.

## Tech Stack

Backend:

* Java 21
* Spring Boot 3.x
* Spring Web
* Spring Data JPA
* Spring WebClient
* MySQL
* Maven

Frontend:

* React
* Vite
* Ant Design
* Axios

External services:

* GitHub REST API
* OpenAI-compatible model API

## Repository Structure

Use this monorepo structure:

```text
pr-review-agent/
├── backend/
├── frontend/
├── docs/
├── README.md
├── AGENTS.md
└── .gitignore
```

Rules:

* Backend code must stay under `backend/`.
* Frontend code must stay under `frontend/`.
* Project documents must stay under `docs/`.
* Do not mix frontend and backend source code in the repository root.
* `screenshots/` is optional and only used for demo images.

## Reference Documents

Before implementing a task, check the relevant documents in `docs/`.

Important documents:

* `docs/需求分析.md`
* `docs/技术选型.md`
* `docs/架构设计.md`
* `docs/数据库设计.md`
* `docs/模块拆分与开发计划.md`

Use `docs/模块拆分与开发计划.md` as the main development roadmap.

Do not copy all document content into code comments. Use the documents as implementation guidance.

## Development Rules

* Implement only the currently requested module.
* Each PR must do only one thing.
* Prefer small, reviewable changes.
* Do not implement future modules early.
* Do not modify unrelated files.
* Do not delete existing docs or tests unless explicitly asked.
* Keep the main branch runnable after every PR.
* If a prerequisite module is missing, report it instead of silently building around it.

## Module Order

Develop the project in this order:

1. Project structure
2. Backend base service
3. Frontend base page
4. Database entities and repositories
5. GitHub PR data fetching
6. Diff parsing and review context building
7. AI model client
8. AI Review analysis
9. Review task APIs
10. Frontend review workflow and result display
11. Validation and exception handling
12. Documentation and demo materials

Only work on the module explicitly requested by the user.

## Backend Guidelines

Use this package structure under `backend/src/main/java/...`:

```text
controller/
service/
client/
repository/
entity/
dto/
config/
exception/
enums/
util/
```

Rules:

* Controllers handle request and response only.
* Business logic belongs in services.
* GitHub API calls belong in `GitHubClient`.
* AI model API calls belong in `ModelClient`.
* Database access belongs in repositories.
* Use DTOs for API request and response objects.
* Do not expose JPA entities directly to the frontend.
* Read configuration from `application.yml`, environment variables, or local ignored config files.
* Do not hardcode API keys, tokens, or secrets.

## Frontend Guidelines

Use this structure under `frontend/src/`:

```text
api/
pages/
components/
utils/
App.jsx
main.jsx
```

Rules:

* API request functions belong in `api/`.
* Page components belong in `pages/`.
* Reusable UI components belong in `components/`.
* Utility functions belong in `utils/`.
* Show loading states for async operations.
* Show clear error messages when requests fail.
* Do not store GitHub tokens or model API keys in frontend code.

## Database Guidelines

Use MySQL and Spring Data JPA.

Core tables:

* `review_task`
* `pull_request_info`
* `review_result`
* `risk_item`
* `model_call_log`

Rules:

* Follow `docs/数据库设计.md` for table and field design.
* Use `task_id` to connect review-related records.
* Task status values: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`.
* Risk level values: `HIGH`, `MEDIUM`, `LOW`.
* Do not store API keys or GitHub tokens in the database.
* Store PR metadata, review results, risk items, and model call logs only.

## GitHub API Guidelines

GitHub integration should be isolated in `GitHubClient`.

Required capabilities:

* Parse `repoUrl` into owner and repository name.
* Fetch PR metadata by PR number.
* Fetch changed files for a PR.
* Read patch/diff content from changed files.
* Support public repositories without a token when possible.
* Support optional GitHub token from configuration.
* Return clear errors for missing PRs, invalid repositories, rate limits, and GitHub API failures.

Do not call GitHub APIs directly from controllers.

## AI Model Guidelines

AI model integration should be isolated in `ModelClient`.

Rules:

* Use an OpenAI-compatible chat completion API.
* API base URL, model name, and API key must be configurable.
* Never hardcode model credentials.
* Log model call metadata in `model_call_log`.
* Handle timeout, API failure, and invalid response cases.
* Prefer structured JSON output for AI Review results.
* Add fallback handling when model output cannot be parsed.

## AI Review Guidelines

The AI Review output should include:

* PR summary
* changed modules
* risk items
* review suggestions
* test suggestions
* overall conclusion

Risk items should include:

* file path
* line number when available
* risk level
* risk type
* description
* suggestion
* confidence when available

Avoid noisy review comments. Do not force problems when the diff does not provide enough evidence. For uncertain findings, phrase them as suggestions to confirm.

Focus on:

* bugs
* exception handling
* input validation
* security risks
* performance issues
* database changes
* configuration changes
* missing tests

## Security Rules

Never commit:

* `.env`
* `application-local.yml`
* API keys
* GitHub tokens
* model provider keys
* passwords
* local IDE secrets

Secrets must be loaded from environment variables or ignored local config files.

`.gitignore` must cover common generated files and sensitive files, including:

```text
node_modules/
target/
.env
application-local.yml
.idea/
.vscode/
```

## Commit and PR Rules

Commit message format:

```text
<type>: <description>
```

Allowed types:

* `feat`
* `fix`
* `docs`
* `chore`
* `test`
* `refactor`
* `style`

Examples:

```text
feat: add github api client
feat: add review task entity
fix: handle github api error response
docs: update project readme
```

Each PR must include:

1. Title: one sentence describing the change.
2. Feature description: what the change does and how to use it.
3. Implementation idea: core design or technical approach.
4. Test method: how to verify the change.

Each PR must keep the project runnable.

## Test Commands

Backend:

```bash
cd backend
mvn spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

If a task adds an API, provide an example request in the final response.

If a task changes database behavior, describe how to verify the tables or records.

## Required Response After Coding

After completing any coding task, respond with:

```text
## Completed
Briefly describe what was done.

## Files Changed
List added or modified files and their purpose.

## How to Test
Provide exact commands or API requests.

## Suggested Commit Message
Provide 1 to 3 commit messages.

## Notes
Mention required environment variables, database setup, or limitations.
```

## Do Not

* Do not implement multiple modules in one task.
* Do not rewrite the whole project unless explicitly asked.
* Do not add unrelated dependencies.
* Do not move files across frontend/backend without a clear reason.
* Do not hardcode secrets.
* Do not silently ignore failed tests or startup errors.
* Do not claim a feature is complete without a test method.
* Do not generate broad, noisy AI Review results that are unrelated to the PR diff.
