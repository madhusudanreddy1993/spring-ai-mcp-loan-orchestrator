# 🏦 spring-ai-mcp-loan-orchestrator

> **AI-Orchestrated Loan Processing System (PoC)** — A production-inspired reference implementation demonstrating Spring AI tool orchestration, MCP (Model Context Protocol) integration, Drools-based rule governance, and auditable AI workflow tracing..

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.0--M1-blue)](https://spring.io/projects/spring-ai)
[![Drools](https://img.shields.io/badge/Drools-8.44.2-red)](https://www.drools.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 📌 Project Overview

### The Problem

Modern loan processing systems face a fundamental tension: AI models are excellent at reasoning over complex, multi-dimensional data — but they are probabilistic, non-deterministic, and cannot be trusted as sole decision authorities in regulated financial workflows. At the same time, pure rule-based systems lack the flexibility and contextual reasoning that AI provides.

### The Solution

This project demonstrates a **hybrid AI + governance architecture** where:

- An **LLM (via Spring AI)** orchestrates a multi-step tool-calling workflow to gather and reason over applicant data
- A **Drools rules engine** acts as the non-negotiable backend authority, capable of overriding AI decisions when business rules are violated
- Every tool invocation, decision, and workflow step is **fully auditable** via persistent tracing
- The system is **MCP-compliant**, exposing tools both to the AI agent and to the MCP Inspector for independent testing and observability

This project is a production-inspired reference implementation demonstrating enterprise AI orchestration patterns for regulated and auditable financial workflows. While intentionally simplified as a PoC, the architecture emphasizes real-world concerns such as governance, observability, transaction boundary management, and deterministic backend rule enforcement.

---

## 🏗️ Architecture Overview

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                            │
│              LoanController (/loan/apply)                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  AI Orchestration Layer                      │
│    LoanAgentService (ChatClient + @Tool binding)            │
│    Prompt built from external template + app context        │
└──────┬───────────────────────────────────────┬──────────────┘
       │  LLM orchestrates tool calls          │ Drools runs
       ▼                                       ▼ independently
┌──────────────────┐                  ┌────────────────────┐
│   ApiLoanTools   │                  │  RulesEngineService│
│  (Tool calling)  │                  │  (Drools DRL)      │
└──────┬───────────┘                  └────────────────────┘
       │ delegates to
┌──────▼───────────┐
│ LoanToolsDelegate│ ← Shared business logic
└──────┬───────────┘
       │ persists via
┌──────▼───────────┐
│  ToolTraceLogger │ → tool_trace (DB)
└──────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  MCP Server (parallel path)                  │
│    McpLoanTools exposed at /mcp for Inspector access        │
│    Same delegate, independent persistence, inspector format │
└─────────────────────────────────────────────────────────────┘
```

### Request Flow

```
POST /loan/apply
    │
    ├── 1. Save LoanApplication (outside TX — survives AI failure)
    │
    ├── 2. Build prompt (template + app data + workflow context)
    │
    ├── 3. ChatClient.prompt().call() ── LLM invokes tools:
    │       ├── validateAge        (STEP 1 — fail fast on age < 21)
    │       ├── fetchCreditScore   (STEP 2 — fetch/assign credit score)
    │       ├── performFraudCheck  (STEP 3 — fail fast on fraud)
    │       └── evaluateLoanRules  (STEP 4 — Drools via tool)
    │
    ├── 4. Parse AI JSON response → LoanDecisionResponse
    │
    ├── 5. Run Drools independently (authoritative backend check)
    │
    └── 6. persistDecision() [@Transactional]
            ├── Drools override check
            ├── Save LoanDecision + WorkflowStatus
            └── Audit log
```

# Key Separation of Concerns

| Layer | Responsibility |
|---|---|
| `LoanController` | HTTP in/out, status code selection (200 vs 422) |
| `LoanAgentService` | AI orchestration, transaction boundary management |
| `ApiLoanTools` | `@Tool` methods bound to `ChatClient` |
| `McpLoanTools` | `@Tool` methods exposed to MCP Inspector |
| `LoanToolsDelegate` | Actual business logic — shared by both tool implementations |
| `RulesEngineService` | Drools session lifecycle, independent evaluation |
| `ToolTraceLogger` | Persistent audit of every tool invocation |

---

## ✨ Key Features

- **Sequential AI Tool Orchestration** — LLM coordinates four mandatory tools in strict sequence using Spring AI's `ChatClient` and `@Tool` annotations
- **MCP Server Integration** — Exposes loan tools via the Model Context Protocol over HTTP (Streamable transport), enabling MCP Inspector access and future MCP client integrations
- **Drools Rules Engine Governance** — Backend DRL rules are final authority; AI approvals can be overridden if business rules fail
- **Dual Tool Implementation** — `ApiLoanTools` for AI-driven orchestration; `McpLoanTools` for direct MCP Inspector invocation, sharing logic via `LoanToolsDelegate`
- **Persistent Audit Tracing** — Every tool call is persisted to `tool_trace` table with tool name, input, output, duration, and FK link to the loan application
- **Transaction Boundary Safety** — LLM calls are intentionally outside database transactions; only fast DB writes are transactional, preventing connection pool exhaustion
- **Externalized Prompt Templates** — Prompts live in `resources/prompt-templates/` as plain `.txt` files, loaded at runtime, editable without recompilation
- **Typed Decision Responses** — `LoanDecisionResponse` replaces brittle string parsing; `JsonParserUtil` safely strips LLM markdown fences
- **WorkflowStatus Tracking** — `PENDING → IN_PROGRESS → APPROVED / REJECTED / OVERRIDDEN` lifecycle persisted per decision
- **Fail-Safe Defaults** — Any parse failure or ambiguous LLM output defaults to rejection, never to unsafe approval

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| AI Orchestration | Spring AI 1.1.0-M1 |
| LLM Provider | Together AI (OpenAI-compatible) — `Qwen/Qwen2.5-7B-Instruct-Turbo` |
| MCP Protocol | Spring AI MCP Server (Streamable HTTP) |
| Rules Engine | Drools 8.44.2 (drools-engine-classic) |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 (file-based, dev) / PostgreSQL or MySQL (production) |
| Build | Maven |
| Observability | Custom ToolTraceLogger → `tool_trace` table |
| Containerization | Docker (recommended for production) |
| Cloud | AWS (recommended deployment target) |
| Messaging | Kafka (recommended for async decision events in production) |

---

## 📁 Project Structure

```
src/main/java/com/example/loanmcp/
├── LoanMcpApplication.java          # Spring Boot entry point
│
├── audit/
│   ├── DecisionAudit.java           # In-memory audit record (DTO)
│   ├── DecisionAuditService.java    # In-memory audit service used for demo observability
│   ├── ToolTrace.java               # JPA entity for persistent tool traces
│   └── ToolTraceLogger.java         # Persists traces, links to LoanApplication
│
├── config/
│   ├── DroolsConfig.java            # KieContainer bean, DRL validation at startup
│   └── ToolConfig.java              # Registers McpLoanTools with MCP server
│
├── controller/
│   └── LoanController.java          # REST endpoints: /apply, /audit
│
├── domain/
│   ├── LoanApplication.java         # JPA entity — applicant data
│   ├── LoanDecision.java            # JPA entity — decision outcome + status
│   └── WorkflowStatus.java          # Enum: PENDING/IN_PROGRESS/APPROVED/REJECTED/OVERRIDDEN
│
├── model/
│   ├── LoanDecisionResponse.java    # Typed AI response DTO
│   └── LoanWorkflowContext.java     # Workflow state passed to LLM prompt
│
├── repository/
│   ├── LoanApplicationRepository.java
│   ├── LoanDecisionRepository.java
│   └── ToolTraceRepository.java     # findByLoanApplicationId, findByToolName
│
├── service/
│   ├── LoanAgentService.java        # AI orchestration, TX boundary management
│   └── RulesEngineService.java      # Drools KieSession lifecycle
│
├── tools/
│   ├── ApiLoanTools.java            # @Tool methods for ChatClient (AI path)
│   ├── McpLoanTools.java            # @Tool methods for MCP Inspector
│   ├── LoanToolsDelegate.java       # Shared business logic (no duplication)
│   ├── LoanToolsPort.java           # Interface contract for both tool implementations
│   ├── LoanToolsFactory.java        # Runtime context router (API vs Inspector)
│   └── ToolResult.java              # Generic typed wrapper for all tool responses
│
└── util/
    └── JsonParserUtil.java          # Safe LLM response parser, strips markdown fences

src/main/resources/
├── application.properties
├── prompt-templates/
│   └── loan-evaluation.txt          # Externalized LLM prompt — editable at deploy time
└── rules/
    └── loan-rules.drl               # Drools decision rules
```

---

## 🔄 Workflow Explanation

### End-to-End Loan Evaluation

**Step 1 — Application Receipt**
`POST /loan/apply` receives a `LoanApplication` JSON body. The controller delegates immediately to `LoanAgentService.evaluateLoan()`.

**Step 2 — Persistence Before AI**
The application is saved to the database *before* the LLM call. This ensures the application record survives even if the AI call fails, and gives the FK reference needed by `ToolTrace` records.

**Step 3 — Prompt Construction**
`buildPrompt()` loads `loan-evaluation.txt` from the classpath and substitutes the application data and workflow context using `String.format()`. The prompt instructs the LLM to invoke exactly four tools in strict sequence.

**Step 4 — AI Tool Orchestration**
`ChatClient` sends the prompt to the LLM. The model calls tools in sequence:
1. `validateAge` — age eligibility check; stops workflow on failure
2. `fetchCreditScore` — credit score lookup; uses applicant-provided score or simulates bureau lookup
3. `performFraudCheck` — fraud detection; stops workflow if fraud detected
4. `evaluateLoanRules` — invokes Drools via tool; returns all triggered rule outcomes

Each tool wraps its result in `ToolResult<T>` and persists a `ToolTrace` record.

**Step 5 — Independent Drools Evaluation**
Separately from the tool calls, `RulesEngineService.evaluate()` runs the same Drools session independently. This is the authoritative governance check.

**Step 6 — Decision Persistence**
`persistDecision()` (annotated `@Transactional`) receives the AI decision and Drools outcomes. If the AI approved but Drools rejected, the status becomes `OVERRIDDEN`. The `LoanDecision` is saved with the final status.

---

## 🔑 Important Design Decisions

### Why is the LLM call outside a database transaction?

LLM inference takes 2–10 seconds. Wrapping it in a `@Transactional` method would hold a database connection open for that entire duration. Under any real load, this exhausts the connection pool and causes request queuing or failures. The pattern used here — persist application first (outside TX), call LLM, then commit decision in a short `@Transactional` method — ensures connection pool efficiency.

### Why are ApiLoanTools and McpLoanTools separate?

These serve fundamentally different callers with different needs:
- `ApiLoanTools` is bound to `ChatClient` and assumes the application is already persisted and the LLM is orchestrating the sequence
- `McpLoanTools` is registered with the MCP server and must handle tool calls from the MCP Inspector in isolation — it must persist the application itself if needed and format responses for human readability

Both delegate to `LoanToolsDelegate` for all business logic, eliminating code duplication.

### Why does Drools override AI decisions?

AI models are probabilistic and can be hallucinated or manipulated via adversarial prompts. In regulated financial workflows, governance rules must be deterministic and auditable. Drools rules are version-controlled, testable, and cannot be bypassed by prompt engineering. The AI provides reasoning and orchestration; Drools provides governance.

### Why are prompts externalized to `.txt` files?

Prompts are first-class engineering artifacts. Externalizing them to `resources/prompt-templates/loan-evaluation.txt` means prompt changes don't require Java recompilation. They can be reviewed in version control separately from application logic. The choice of plain `.txt` over `.st` (StringTemplate) is deliberate — Spring AI's `PromptTemplate` uses StringTemplate 4, which has parsing conflicts with Drools-style curly-brace expressions.

### Why is ToolResult<T> a generic wrapper?

All four tools return structurally different data types (`String`, `Integer`, `Boolean`, `List<String>`). A generic `ToolResult<T>` gives the LLM a consistent JSON envelope for every tool, making orchestration prompts simpler and more reliable. It also carries metadata (duration, timestamp, error) that feeds directly into `ToolTraceLogger`.

---

## 🤖 AI Tool Orchestration Flow

### Tool Registration

`ApiLoanTools` is registered with `ChatClient` via `builder.defaultTools(apiTools)` in `LoanAgentService`. Spring AI reflects over `@Tool`-annotated methods and generates JSON schemas for each, which are passed to the LLM as available tools.

`McpLoanTools` is registered with the MCP server via `MethodToolCallbackProvider` in `ToolConfig`, making it available at the `/mcp` endpoint for the MCP Inspector.

### Tool Sequencing via Prompt Engineering

The sequencing discipline is enforced via the system prompt, not code logic. The prompt explicitly numbers the tools, mandates execution order, specifies stop conditions (age failure, fraud detection), and prohibits the model from generating a final decision before `evaluateLoanRules` completes.

```
1. validateAge      → fail fast if age < 21
2. fetchCreditScore → always runs after step 1
3. performFraudCheck → fail fast if fraud detected
4. evaluateLoanRules → final Drools evaluation
```

### Observability

Every tool invocation writes a `ToolTrace` record to the database:
- `toolName` — which tool was called
- `input` — serialized `LoanApplication` at time of call
- `output` — serialized result
- `durationMs` — execution time in milliseconds
- `timestamp` — UTC invocation time
- `loan_application_id` — FK for efficient per-application queries

---

## 🚀 Installation & Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- A Together AI API key (or any OpenAI-compatible endpoint)
- (Optional) Docker for containerized deployment

### Clone

```bash
git clone https://github.com/madhusudanreddy1993/spring-ai-mcp-loan-orchestrator.git
cd spring-ai-mcp-loan-orchestrator
```

### Configure

Open `src/main/resources/application.properties` and set your API key:

```properties
spring.ai.openai.base-url=https://api.together.xyz
spring.ai.openai.api-key=YOUR_TOGETHER_AI_KEY_HERE
spring.ai.openai.chat.options.model=Qwen/Qwen2.5-7B-Instruct-Turbo
spring.ai.openai.chat.options.temperature=0.2

spring.datasource.url=jdbc:h2:file:./data/loandb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.protocol=STREAMABLE
spring.ai.mcp.server.stdio=false
spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp
spring.ai.mcp.server.type=SYNC
```

### Build and Run

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

The app starts at `http://localhost:8080`.

- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/loandb`, username: `sa`, password: empty)
- MCP endpoint: `http://localhost:8080/mcp`

### Connect MCP Inspector

If you have the MCP Inspector installed:

```bash
npx @modelcontextprotocol/inspector
```

Point it at `http://localhost:8080/mcp` using Streamable HTTP transport. You'll see all four loan tools available for direct invocation.

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/loan/apply` | Submit a loan application for AI evaluation |
| `GET` | `/loan/audit` | Retrieve all in-memory decision audit records |
| `GET` | `/h2-console` | H2 database console (development only) |
| `POST/GET` | `/mcp` | MCP Streamable HTTP endpoint for Inspector |

---

## 📋 Sample Request / Response

### Request — Loan Application

```json
POST /loan/apply
Content-Type: application/json

{
  "age": 28,
  "income": 75000,
  "existingLoanAmount": 15000,
  "creditScore": 720
}
```

### Response — Approved (HTTP 200)

```json
{
  "approved": true,
  "reason": "All validation checks passed. Credit score of 720 meets the minimum threshold. No fraud indicators detected. Drools rules confirmed eligibility: income and debt ratios within acceptable bounds.",
  "ruleOutcomes": ["APPROVED"]
}
```

### Response — Rejected (HTTP 422)

```json
{
  "approved": false,
  "reason": "Application rejected. Credit score of 540 is below the minimum required score of 600. Drools rules triggered: REJECT_LOW_CREDIT.",
  "ruleOutcomes": ["REJECT_LOW_CREDIT"]
}
```

### Response — AI Override (HTTP 422)

```json
{
  "approved": false,
  "reason": "AI approved but overridden by rules engine: [REJECT_LOW_CREDIT, REJECT_HIGH_DEBT]",
  "ruleOutcomes": ["REJECT_LOW_CREDIT", "REJECT_HIGH_DEBT"]
}
```

**Underage applicant — HTTP 422:**

```json
{
  "approved": false,
  "reason": "Validation failed at step 1. Applicant age 19 is below the minimum required age of 21. Application rejected.",
  "ruleOutcomes": ["REJECT_UNDERAGE"]
}
```

**Fraud detected — HTTP 422:**

```json
{
  "approved": false,
  "reason": "Fraud check triggered at step 3. Application exhibits suspicious income-to-debt pattern. Processing halted.",
  "ruleOutcomes": []
}
```

---

### GET /loan/audit

Returns all in-memory decision audit records for the current session.

```json
[
  {
    "applicantId": "1",
    "approved": true,
    "rawResponse": "{\"approved\": true, \"reason\": \"...\"}",
    "timestamp": "2025-01-15T10:23:41.123Z"
  }
]
```

---

### GET /h2-console

H2 database console. Useful for inspecting `loan_application`, `loan_decision`, and `tool_trace` tables directly.

---

### /mcp (Streamable HTTP)

MCP endpoint for Inspector access. All four tools (`validateAge`, `fetchCreditScore`, `performFraudCheck`, `evaluateLoanRules`) are available here for direct invocation.

---

## Typical Test Scenarios

**Happy path — everything passes:**
```json
{ "age": 30, "income": 80000, "existingLoanAmount": 10000, "creditScore": 750 }
```

**Underage — stops at step 1:**
```json
{ "age": 18, "income": 80000, "existingLoanAmount": 0, "creditScore": 750 }
```

**Low credit — passes validation, fails Drools:**
```json
{ "age": 30, "income": 80000, "existingLoanAmount": 10000, "creditScore": 550 }
```

**Fraud trigger — stops at step 3:**
```json
{ "age": 30, "income": 500, "existingLoanAmount": 100000, "creditScore": 750 }
```

**Low income — passes all tools, fails Drools:**
```json
{ "age": 30, "income": 20000, "existingLoanAmount": 5000, "creditScore": 750 }
```

**High debt ratio — passes tools, fails REJECT_HIGH_DEBT:**
```json
{ "age": 30, "income": 40000, "existingLoanAmount": 250000, "creditScore": 750 }
```

---

## 🔮 Future Improvements

**Production Readiness**
- Replace H2 with PostgreSQL or MySQL; migrate schema via Flyway or Liquibase
- Replace Together AI with a production LLM endpoint (Azure OpenAI, Bedrock Claude, etc.)
- Externalize API keys to AWS Secrets Manager or Vault

**Observability**
- Integrate Micrometer + Prometheus for tool duration metrics
- Add distributed tracing via OpenTelemetry
- Expose `ToolTrace` data via a dedicated audit REST API with pagination

**Scalability**
- Publish loan decisions to Kafka for downstream consumers (notification service, CRM, reporting)
- Add async evaluation path with `CompletableFuture` for high-throughput scenarios

**AI Reliability**
- Add retry logic with exponential backoff for LLM API failures
- Implement structured output schemas (JSON schema enforcement) to reduce parse failures
- Add prompt versioning and A/B testing infrastructure

**Security**
- Add Spring Security with JWT for API authentication
- Implement role-based access control for MCP Inspector endpoints
- Add input validation and rate limiting at the controller layer

**Testing**
- Integration tests with `@SpringBootTest` and Testcontainers (PostgreSQL, mock LLM)
- Drools unit tests via `KieSession` directly
- Contract tests for MCP tool schemas

---

## 🙋 Author

Built as a portfolio-grade reference implementation demonstrating enterprise AI orchestration patterns in Java/Spring Boot. Contributions, issues, and discussions are welcome.
