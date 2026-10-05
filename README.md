# money-flow

## What problem it solves

A bank statement is a list of individual transactions. It shows what
happened, but not the answer to the simpler question: how did money move
this month? How much came in, how much went out, and where did the balance
end up? Answering that means adding up the transactions with the same rules
every time: what counts as income, what counts as spending.

money-flow does that for one account and one month. It fetches the statement
from an external statements API, calculates total income, total spending,
the monthly balance and the closing balance, sends that summary to an
external summaries API, and returns the same summary to the caller. Whatever
reads those summaries (for example, an app showing the account holder a
monthly overview) gets ready-made totals instead of raw transactions. There
is no database: every request is calculated fresh from the statement.

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
The two external API addresses and their timeouts are configuration
properties, overridable by environment variables:

```
MONEY_FLOW_STATEMENTS_API_BASE_URL=https://your-statements-api.example.com
MONEY_FLOW_SUMMARY_API_BASE_URL=https://your-summary-api.example.com
MONEY_FLOW_STATEMENTS_API_TIMEOUT=5s   # default 5s
MONEY_FLOW_SUMMARY_API_TIMEOUT=5s      # default 5s
```

If a URL or timeout is missing or blank, the app refuses to start and names
the setting, instead of failing on the first request.

Set them either in `docker-compose.yml` (replacing the WireMock URL), or
directly when running the jar:

```bash
./mvnw package -DskipTests
MONEY_FLOW_STATEMENTS_API_BASE_URL=https://your-statements-api.example.com \
MONEY_FLOW_SUMMARY_API_BASE_URL=https://your-summary-api.example.com \
java -jar target/money-flow-0.0.1-SNAPSHOT.jar
```

## Architecture

The app follows the ports-and-adapters idea: the business logic in the middle
doesn't know how data comes in or goes out. It only talks to two interfaces
it owns, `StatementClient` ("give me a statement") and `SummaryPublisher`
("send this summary"). Those interfaces are the **ports**. The classes that
do the actual HTTP work are the **adapters**: `SummaryController` on the
incoming side, and `HttpStatementClient` and `HttpSummaryPublisher` on the
outgoing side.

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

Why this fits here: neither external API existed yet, so the business logic
was built and tested against simple fakes of the two ports first, before any
HTTP code was written. And if the real APIs turn out different from the
proposed contract, only the two adapter classes change.

It's a light version of the pattern: one Maven module with packages instead
of separate modules, and Spring annotations on the service classes. That's
enough to keep the business logic independent of HTTP without extra
structure an app this size doesn't need.

**Model** (`com.example.moneyflow.model`)

| Class | Job |
|---|---|
| `Transaction` | One transaction on a statement. |
| `Statement` | One bank statement for one account and month. |
| `MonthlySummary` | The totals for one month, sent to the summary API. |

**Business logic and ports** (`com.example.moneyflow.service`)

| Class | Job |
|---|---|
| `StatementCalculator` | Turns a statement into income, spending and balance totals. |
| `StatementClient` (interface) | Gets a statement from the external statements API. |
| `SummaryPublisher` (interface) | Sends a monthly summary to the external summary API. |
| `MoneyFlowService` | Fetches a statement, calculates the summary, and sends it. |

**Outgoing adapters: HTTP clients** (`com.example.moneyflow.client`)

| Class | Job |
|---|---|
| `StatementsApiProperties` | Settings for connecting to the statements API, checked at startup. |
| `SummaryApiProperties` | Settings for connecting to the summary API, checked at startup. |
| `RestClients` | Builds an HTTP client with a base URL and a timeout, shared by both HTTP clients. |
| `AccountIds` | Masks account ids for logs, and names a failure's cause without its message. |
| `HttpStatementClient` | Calls the real statements API over HTTP. |
| `HttpSummaryPublisher` | Sends the summary to the real summary API over HTTP. |

**Incoming adapter: web** (`com.example.moneyflow.web`)

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
| `SummaryTimeoutException` | The summary API did not respond in time; the summary may already be stored. |
| `ApiError` | The body returned for any error response. |
| `GlobalExceptionHandler` | Turns known failures into simple error responses. |

## Assumptions

- Account ids contain only letters, digits and `-`, at most 64 characters.
  Anything else is rejected with a 400 before any call upstream, so an id can
  never change the path the app calls (for example `..`).
- Each statement is in one currency; any conversion happens upstream.
- A positive amount is income, a negative amount is spending, zero is ignored.
- The statements API returns only the requested month's transactions (by
  value date); the app doesn't filter them again.
- Transaction ids are unique within a statement. The app doesn't remove
  duplicates: a transaction listed twice would be counted twice.
- The statement's opening balance is correct. The closing balance is
  calculated as opening balance + income − spending.
- The statement must be for the account and month that were requested.
  Anything else is treated as a bad response (502), not used.
- Only the fields the calculation needs are read; unknown fields are ignored.
  A missing required field is treated as a bad response (502), never as zero.
  The `Statement` and `Transaction` records reject missing fields themselves,
  so every adapter gets the same check, not only the HTTP one.
- Calling the endpoint twice for the same account and month sends the summary
  twice. The summaries API is assumed to accept that, for example by replacing
  the earlier summary for that account and month.
- If the summary `POST` times out, the summary may already be stored even
  though the caller got a 504. Retrying is safe only because the summaries
  API is assumed to replace a summary by account and month.

## Building before the APIs exist

Neither external API exists yet, so the work was set up to not depend on them:

1. **Contract first.** [`openapi.yaml`](./openapi.yaml) is my proposed
   contract for the two APIs this app calls:
   - `GET /accounts/{accountId}/statements/{month}` (the statements API)
   - `POST /monthly-summaries` (the summaries API)

   In a real project it would be agreed with the teams that own those APIs
   before anyone builds against it. This app's own endpoint has its contract
   in [`money-flow-api.yaml`](./money-flow-api.yaml).
2. **Ports with fakes.** The business logic only depends on the
   `StatementClient` and `SummaryPublisher` interfaces, so it was built and
   tested with simple fakes before any HTTP code existed.
3. **WireMock stubs.** The HTTP clients are tested against WireMock playing
   each API according to the contract, and `docker compose up` runs the whole
   app against a WireMock container (stubs in `wiremock/mappings`), so it can
   be tried end to end today.

If the real APIs turn out different, the base URLs are already configuration.
Different paths or fields would need a code change, but only in the two
adapter classes, `HttpStatementClient` and `HttpSummaryPublisher`. Nothing
else in the app depends on them.

## Error handling

| Situation | Status |
|---|---|
| Invalid or missing `accountId`/`month` (`accountId`: letters, digits and `-`, at most 64 characters) | 400 |
| Statements API has no statement for this account/month | 404 |
| Unknown path (for example `/favicon.ico`) | 404 |
| Wrong HTTP method (anything other than `POST`) | 405 |
| Unexpected error inside the app | 500 |
| Statements API fails, or returns an incomplete statement or one for a different account/month | 502 |
| Summary API fails (the summary is not returned in this case) | 502 |
| Statements API or summary API times out | 504 |

All error responses share the same simple body: `{"message": "..."}`. The
exact messages per status are in [`money-flow-api.yaml`](./money-flow-api.yaml).
Messages never include internal details or the account id.

Every failed request is logged by the error handler, with the status the
caller got. A failed call to an external API also logs one line from the
HTTP client: the account id masked to its last 4 characters (`****` for ids
of 4 characters or fewer) and the kind of failure, by exception type only.
Upstream error bodies and URLs are never logged, because they can contain the
full account id; a test checks this for timeouts, upstream errors and 404s.
Stack traces are logged only for unexpected errors (500), where they help find
a bug; every other error logs one line.

## Testing

Requires Java 25 on your `PATH` (or `JAVA_HOME` pointed at it). Class files
built with Java 25 won't run on an older JVM. This only matters for running
tests locally; `docker compose up --build` bundles its own Java 25 and is
unaffected by what's on your machine.

```bash
./mvnw test
```
Runs all 65 tests without Docker or a running app. CI runs the same tests
with `./mvnw verify` and then builds the Docker image.

Seven levels, each with the tool that fits it:

- **Model** (`StatementTest`, `TransactionTest`): plain unit tests, no
  framework. The records reject missing fields and keep their own unchangeable
  list of transactions.
- **Calculator** (`StatementCalculatorTest`): plain numbers in, plain numbers
  out, no framework. Built with real TDD for the first two rules (a failing
  test, then the minimum code to pass); the rest passed immediately once the
  general formula existed, so those were committed as confirmation tests
  rather than faked failures.
- **Service** (`MoneyFlowServiceTest`): hand-written Java fakes for
  `StatementClient`/`SummaryPublisher`, no mocking framework. Checks the
  order (fetch, calculate, send) and that a statement for the wrong account
  or month is rejected.
- **HTTP clients** (`HttpStatementClientTest`, `HttpSummaryPublisherTest`):
  WireMock plays each external API. Covers success (including the exact JSON
  sent to the summaries API), 404, 500, timeouts, refused and reset
  connections, incomplete responses, unknown fields being ignored, and that
  the account id never ends up in an error message or unmasked in a log.
- **Web and error handling** (`SummaryControllerTest`,
  `GlobalExceptionHandlerTest`): MockMvc with Mockito's `@MockitoBean`
  standing in for `MoneyFlowService`. Covers every status
  (200/400/404/405/500/502/504, including unknown paths) with its exact
  message, and that stack traces are logged only for unexpected errors.
  Mockito is used only at this layer: it's the idiomatic tool for testing
  Spring's HTTP wiring, while the service layer uses plain fakes since its
  logic is simple and pure.
- **Configuration** (`ApiPropertiesValidationTest`, `MoneyFlowApplicationTests`):
  the app refuses to start with a blank URL or missing timeout, and the full
  application context starts with the real configuration.
- **End to end** (`MoneyFlowEndToEndTest`): starts the whole app on a random
  port, with WireMock playing both external APIs, and calls it over real
  HTTP. Covers success (including the summary JSON the summaries API
  receives), a missing statement (404), and a failing summaries API (502),
  and that the full account id never appears in the logs for timeouts,
  upstream errors and 404s.

To try the whole app by hand, use `docker compose up --build` and the demo
requests in "How to run it".

## Deployment

What exists today:

- A `Dockerfile` that builds a small runtime image: only a Java runtime, no
  build tools or source code, and the app runs as a non-root user.
- CI (GitHub Actions) that runs all tests and builds that image on every pull
  request and every push to `main`.

How I would deploy it to a remote server (none of this is set up here):

1. **Publish the image.** When CI passes on `main`, push the image to a
   container registry, tagged with the commit hash, so every running version
   can be traced back to its code.
2. **Run it on the server.** The server pulls that tagged image and runs it,
   passing the real API URLs as environment variables. The same image goes to
   every environment; only the variables change. For several servers, or
   automatic restarts and scaling, an orchestrator like Kubernetes does the
   same job.
3. **Put HTTPS in front.** The app serves plain HTTP. A load balancer or
   reverse proxy in front of it would handle HTTPS (encryption and the
   certificate), and pass requests on to the app inside the private network.
4. **Add a health check.** Spring Boot Actuator's `/actuator/health` endpoint
   would let the server or orchestrator see whether the app is up and restart
   it if not. Not added yet: it's a new dependency, and nothing uses it today.
5. **Roll back by tag.** Because each image is tagged with its commit, going
   back means running the previous tag again.

## Trade-offs and next steps

Trade-offs I made on purpose:

- **One synchronous call does everything.** Fetch, calculate and send happen
  inside one request, with no database or queue (the task asks for no
  persistence). It's simple, but if the summaries API is down or slow the
  caller gets a 502 or 504 and has to try again later; nothing is kept to retry
  automatically.
- **A plain HTTP client builder instead of Spring Boot's ready-made one.**
  Both HTTP clients build their `RestClient` from `RestClient.builder()`
  through one small shared helper. Spring Boot 4 can provide a pre-configured
  builder that follows the app's JSON settings, but only with an extra
  module (`spring-boot-restclient`). The app has no custom JSON settings, so
  the result is the same today; worth switching if that changes.

Next steps:

- A scheduled trigger (for example, once a month per account) instead of
  only reacting to a manual request.
- Retries with backoff and a circuit breaker, for short and longer outages.
  Retrying the summary `POST` needs care: sending the same summary twice is
  only safe if the summaries API handles duplicates (see Assumptions).
- Authentication on this app's own endpoint, and on its calls to the two
  external APIs.
- Metrics and alerting. The error handler logs every failure, but nothing
  counts them or raises an alert.
- The deployment steps described above: pushing the image to a registry, a
  real deploy step, and a health check endpoint.

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
