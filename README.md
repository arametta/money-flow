# money-flow

## What problem it solves

A bank statement lists single transactions, but it doesn't say how money
moved in the month. money-flow fetches one account's statement for one month
from a statements API, calculates total income, total spending, the monthly
balance and the closing balance, sends that summary to a summaries API, and
returns it to the caller. There is no database: every request is calculated
fresh.

## How to run it

```bash
docker compose up --build
```
This starts the app on `localhost:8080` and a WireMock container on
`localhost:8081` that stubs both external APIs.

```bash
curl -X POST "http://localhost:8080/summaries?accountId=acc-1&month=2026-01"
```

Response:
```json
{"accountId":"acc-1","month":"2026-01","currency":"EUR","totalIncome":1200.00,"totalSpending":345.50,"monthlyBalance":854.50,"openingBalance":500.00,"closingBalance":1354.50}
```

To see what the app sent to the summaries API (every request WireMock
received, with its body):
```bash
curl http://localhost:8081/__admin/requests
```

The stubs include a few more cases to try:

| `accountId` | `month` | What it shows |
|---|---|---|
| `acc-1` | `2026-01` | Income and spending (the example above) |
| `acc-2` | `2026-02` | Spending only: the balance goes down |
| `acc-3` | `2026-03` | Income only, starting from a zero balance |
| anything else | any | 404, no statement found |

### Without Docker for the app
```bash
docker compose up wiremock      # only the stubs, on localhost:8081
./mvnw spring-boot:run          # the app, using the default URLs
```

### Pointing it at real APIs
```
MONEY_FLOW_STATEMENTS_API_BASE_URL=https://your-statements-api.example.com
MONEY_FLOW_SUMMARY_API_BASE_URL=https://your-summary-api.example.com
MONEY_FLOW_STATEMENTS_API_TIMEOUT=5s   # default 5s
MONEY_FLOW_SUMMARY_API_TIMEOUT=5s      # default 5s
```
Set them in `docker-compose.yml` or as environment variables. If a URL or
timeout is missing or blank, the app refuses to start and names the setting.

## Architecture

```
           HTTP request
                │
        SummaryController                 incoming adapter (web)
                │
        MoneyFlowService ── StatementCalculator   business logic (service)
           │          │
  StatementClient   SummaryPublisher      ports (interfaces in service)
           │          │
HttpStatementClient  HttpSummaryPublisher outgoing adapters (client)
           │          │
    statements API   summaries API
```

The app uses ports and adapters: the business logic only talks to two
interfaces it owns, `StatementClient` and `SummaryPublisher` (the ports), and
the HTTP classes implement them (the adapters). Because of this, the business
logic was built and tested with simple fakes before any HTTP code or real API
existed. If the real APIs differ from the proposed contract, only the two
adapter classes change. It's a light version of the pattern: one Maven module
with packages, and Spring annotations on the service classes.

Packages:

- `model`: the `Statement`, `Transaction` and `MonthlySummary` records.
- `service`: the business logic and the two ports.
- `client`: the two HTTP adapters, their settings and small shared helpers.
- `web`: the controller.
- `error`: the domain exceptions and the handler that turns them into HTTP statuses.

How the work started before the APIs existed:

- **Contract first.** [`openapi.yaml`](./openapi.yaml) proposes the two
  external APIs, to be agreed with the teams that own them.
  [`money-flow-api.yaml`](./money-flow-api.yaml) is this app's own contract.
- **Fakes.** The service tests use hand-written fakes of the two ports.
- **WireMock.** The HTTP client tests and `docker compose` use WireMock stubs
  (`wiremock/mappings`), so the whole app runs end to end today.

## Assumptions

- Account ids are 1 to 64 letters, digits or `-`. Anything else gets a 400
  before any call upstream, so an id can't change the upstream path.
- Each statement is in one currency; conversion happens upstream.
- A positive amount is income, a negative amount is spending, zero is ignored.
- The statements API returns only the requested month's transactions (by
  value date); the app doesn't filter them again.
- Transaction ids are unique; a duplicate would be counted twice.
- The opening balance is correct; closing balance = opening + income − spending.
- The statement must match the requested account and month, or the app answers 502.
- Unknown fields are ignored. A missing required field gives 502, never zero;
  the records themselves reject it.
- Calling twice sends the summary twice; the summaries API is assumed to
  replace it by account and month.
- After a summary timeout (504) the summary may already be stored; retrying
  is safe only because of that replace rule.

## Error handling

| Situation | Status |
|---|---|
| Invalid or missing `accountId` or `month` | 400 |
| Statements API has no statement for this account/month | 404 |
| Unknown path (for example `/favicon.ico`) | 404 |
| Wrong HTTP method (anything other than `POST`) | 405 |
| Unexpected error inside the app | 500 |
| Statements API fails, or returns an incomplete statement or one for a different account/month | 502 |
| Summary API fails (the summary is not returned in this case) | 502 |
| Statements API or summary API times out | 504 |

Every error has the body `{"message": "..."}`; the exact messages are in
[`money-flow-api.yaml`](./money-flow-api.yaml). Account ids are masked in logs
(last 4 characters) and never appear in error messages. Stack traces are
logged only for unexpected errors (500).

## Testing

Running the tests locally needs Java 25 (on your `PATH` or in `JAVA_HOME`);
`docker compose` brings its own Java 25.

```bash
./mvnw test
```
Runs all 67 tests without Docker or a running app. CI runs the same tests
with `./mvnw verify` and then builds the Docker image.

- **Model** (`StatementTest`, `TransactionTest`): plain JUnit; the records reject missing fields.
- **Calculator** (`StatementCalculatorTest`): plain JUnit, numbers in, numbers out.
- **Service** (`MoneyFlowServiceTest`): hand-written fakes for the two ports, no Mockito.
- **HTTP clients** (`HttpStatementClientTest`, `HttpSummaryPublisherTest`): WireMock plays each API: success, errors, timeouts, bad responses.
- **Web** (`SummaryControllerTest`, `GlobalExceptionHandlerTest`, `ParameterErrorMessagesTest`): MockMvc with `@MockitoBean` for the service; every status and its message.
- **Configuration** (`ApiPropertiesValidationTest`, `MoneyFlowApplicationTests`): the app refuses to start with bad settings.
- **End to end** (`MoneyFlowEndToEndTest`): the whole app over real HTTP against WireMock, including that account ids never appear in logs.

## Deployment

What exists today:

- A `Dockerfile` that builds a small image: only a Java runtime, and the app
  runs as a non-root user.
- CI (GitHub Actions) that runs all tests and builds the image on every pull
  request and every push to `main`.

How I would deploy it to a remote server (not set up here):

1. **Publish the image** to a container registry when CI passes on `main`, tagged with the commit hash.
2. **Run it on the server** with the real API URLs as environment variables; for several servers, an orchestrator like Kubernetes does the same job.
3. **Put HTTPS in front:** a load balancer or reverse proxy handles HTTPS and passes requests on to the app.
4. **Add a health check:** Spring Boot Actuator's `/actuator/health`. Not added: it's a new dependency and nothing uses it today.
5. **Roll back by tag:** run the previous commit's image again.

## Trade-offs and next steps

Trade-offs I made on purpose:

- **One synchronous call does everything**, with no database or queue (the
  task asks for no persistence). If the summaries API is down or slow, the
  caller gets a 502 or 504 and must try again; nothing retries automatically.
- **A plain `RestClient.builder()` instead of Spring Boot's pre-configured
  builder**, which in Spring Boot 4 needs an extra module
  (`spring-boot-restclient`). With no custom JSON settings, the result is the
  same today.

Next steps:

- A scheduled trigger (for example, once a month per account).
- Retries with backoff and a circuit breaker; retrying the summary `POST` is
  safe only if the summaries API handles duplicates (see Assumptions).
- Authentication on this app's endpoint and on its calls to the two APIs.
- Metrics and alerting: failures are logged, but nothing counts them or alerts.
- The deployment steps above: registry, a real deploy step, a health check.

## Use of AI tools

I used Claude Code as a coding assistant. The plan came first: I split the
work into small phases and decided the architecture, the API contract, the
error handling and the testing approach before any code was written. Claude
Code then helped implement each phase, write the tests and refactor, one
small step at a time. I reviewed every change and every commit message
before it was committed, and kept the history in small commits so each step
can be checked on its own.

Tests were the main check on the generated code. The calculation rules and
the bug fixes after review were done test first: a failing test, then the
code to make it pass. When a new test passed straight away, it was committed
as a confirmation test instead of being presented as a failing one.

When the code was done, I ran several rounds of AI-assisted code review. I
checked each finding against the code, or reproduced it against the running
app, before acting on it. Most were real and were fixed test first; a few I
left on purpose, with the reason written down; and some turned out different
from how they were reported. Findings outside the scope of this task went to
"Trade-offs and next steps".
