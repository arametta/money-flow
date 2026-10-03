# money-flow

This service calculates a monthly summary for one bank account. Given an
account id and a month, it fetches that month's bank statement from an
external API, calculates total income, total spending, the monthly balance,
and the closing balance, then sends the summary to a second external API and
returns the same summary to the caller. There is no database — every request
is calculated fresh from the statement each time.

## How to run it

### With Docker (using the included stubs)
```bash
docker compose up --build
```
This starts the app on `localhost:8080` and a WireMock container stubbing both
external APIs, so you can try it without any real API running.

```bash
curl -X POST "http://localhost:8080/summaries?accountId=acc-1&month=2026-01"
```

Response:
```json
{"accountId":"acc-1","month":"2026-01","currency":"EUR","totalIncome":1200.00,"totalSpending":345.50,"monthlyBalance":854.50,"openingBalance":500.00,"closingBalance":1354.50}
```

### Pointing it at real APIs
The two external API addresses are configuration properties, overridable by
environment variables — nothing is hardcoded:

```
MONEY_FLOW_STATEMENTS_API_BASE_URL=https://your-statements-api.example.com
MONEY_FLOW_SUMMARY_API_BASE_URL=https://your-summary-api.example.com
```

Set them either in `docker-compose.yml` (replacing the WireMock URL), or
directly when running the jar:

```bash
MONEY_FLOW_STATEMENTS_API_BASE_URL=https://your-statements-api.example.com \
MONEY_FLOW_SUMMARY_API_BASE_URL=https://your-summary-api.example.com \
java -jar target/money-flow-0.0.1-SNAPSHOT.jar
```

## Architecture

**Model** (`com.example.moneyflow.model`)

| Class | Job |
|---|---|
| `Transaction` | One transaction on a statement. |
| `Statement` | One bank statement for one account and month. |
| `MonthlySummary` | The totals for one month, sent to the summary API. |

**Business logic** (`com.example.moneyflow.service`)

| Class | Job |
|---|---|
| `StatementCalculator` | Turns a statement into income, spending and balance totals. |
| `StatementClient` (interface) | Gets a statement from the external statements API. |
| `SummaryPublisher` (interface) | Sends a monthly summary to the external summary API. |
| `MoneyFlowService` | Fetches a statement, calculates the summary, and sends it. |

**HTTP clients** (`com.example.moneyflow.client`)

| Class | Job |
|---|---|
| `StatementsApiProperties` | Settings for connecting to the statements API. |
| `SummaryApiProperties` | Settings for connecting to the summary API. |
| `HttpStatementClient` | Calls the real statements API over HTTP. |
| `HttpSummaryPublisher` | Sends the summary to the real summary API over HTTP. |

**Web** (`com.example.moneyflow.web`)

| Class | Job |
|---|---|
| `SummaryController` | Receives a request to build and send one monthly summary. |

**Error handling** (`com.example.moneyflow.error`)

| Class | Job |
|---|---|
| `StatementNotFoundException` | The statements API has no statement for this account and month. |
| `StatementTimeoutException` | The statements API did not respond in time. |
| `StatementUnavailableException` | The statements API failed or returned something we could not use. |
| `SummaryUnavailableException` | The summary API failed, so the summary was not sent. |
| `ErrorResponse` | The body returned for any error response. |
| `GlobalExceptionHandler` | Turns known failures into simple error responses. |

## Assumptions

- The statement arrives in one currency; conversion happens upstream.
- Positive amount is income, negative is spending, zero is ignored.
- The source API returns only transactions of the requested month (by value date).
- Only fields needed for the calculation are modeled; unknown fields are ignored.
- The API contract (`openapi.yaml`) is my own proposal, written before any real
  API team exists. In a real project it would be agreed with the API teams
  first. If the real contract turns out different, only the base URL is a
  runtime config change — the path structure (e.g.
  `/accounts/{accountId}/statements/{month}`) is hardcoded in
  `HttpStatementClient`/`HttpSummaryPublisher` and would need a code change and
  rebuild. The `StatementClient`/`SummaryPublisher` interfaces contain that
  risk: nothing else in the app — the service, the controller, or their tests —
  depends on those two HTTP-specific classes, so a contract change only touches
  two files.

## API contract

The contract for both external APIs this app calls is in
[`openapi.yaml`](./openapi.yaml):

- `GET /accounts/{accountId}/statements/{month}` — the statements API
- `POST /monthly-summaries` — the summary API

## Error handling

| Situation | Status |
|---|---|
| Invalid or missing `accountId`/`month` | 400 |
| Statements API has no statement for this account/month | 404 |
| Statements API fails (5xx or bad response) | 502 |
| Statements API times out | 504 |
| Summary API fails (the summary is not returned in this case) | 502 |

All error responses share the same simple body: `{"message": "..."}`.

## Testing

```bash
./mvnw test
```
Runs every test below without building the Docker image or starting the app.

Four levels, each with the tool that fits it:

- **Calculator** (`StatementCalculatorTest`) — plain numbers in, plain numbers
  out, no framework. Built with TDD: each rule started as a failing test,
  committed before the code that made it pass.
- **Service** (`MoneyFlowServiceTest`) — hand-written Java fakes for
  `StatementClient`/`SummaryPublisher`, no mocking framework, checking the
  orchestration: fetch, calculate, send, in that order.
- **HTTP clients** (`HttpStatementClientTest`, `HttpSummaryPublisherTest`) —
  WireMock, covering success, 404, 500, and timeout for each client.
- **Controller** (`SummaryControllerTest`) — MockMvc with Mockito's
  `@MockitoBean` standing in for `MoneyFlowService`, covering
  200/400/404/502/504. Mockito is used only at this layer — it's the idiomatic
  tool for testing Spring's HTTP wiring, while the service layer above uses
  plain fakes since its logic is simple and pure.

## Deployment

What exists today: a `Dockerfile` that builds a runnable image, and CI that
builds and tests the project plus the Docker image on every push.

What a real deployment would add: push the built image to a container
registry, then have a server (or an orchestrator like Kubernetes) pull and run
it, passing the two API URLs as environment variables. Neither of those is set
up here — the CI step only builds the image, it doesn't push or deploy it
anywhere.

## What I would add next

- A scheduled trigger (e.g. run automatically once a month per account)
  instead of only reacting to a manual request.
- Retries with backoff and a circuit breaker for resilience against transient
  and persistent failures — though retrying the summary `POST` would need
  care, since it's not obviously safe to send the same summary twice unless
  the receiving API is idempotent.
- Authentication on this app's own endpoint, and on the calls it makes to the
  two external APIs.
- More structured logging and metrics (the error handler now logs each
  failure, but there's no metrics/alerting layer).
- Pushing the Docker image to a registry and wiring an actual deploy step,
  instead of only building it in CI.

## Use of AI tools

I used Claude Code as a coding assistant for this task. The design decisions
(architecture, API contract, error handling, testing approach) were made by
me before writing the code, and I reviewed every change and every commit
message before it was committed.
