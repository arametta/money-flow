package com.example.moneyflow;

import com.example.moneyflow.error.ErrorResponse;
import com.example.moneyflow.model.MonthlySummary;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.YearMonth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class MoneyFlowEndToEndTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    @DynamicPropertySource
    static void pointBothApisAtWireMock(DynamicPropertyRegistry registry) {
        registry.add("money-flow.statements-api.base-url", wireMock::baseUrl);
        registry.add("money-flow.summary-api.base-url", wireMock::baseUrl);
        registry.add("money-flow.statements-api.timeout", () -> "500ms");
    }

    private static final String SECRET_ACCOUNT = "acc-secret-12345";

    @LocalServerPort
    private int port;

    private static final String STATEMENT = """
            {
              "accountId": "acc-1",
              "month": "2026-01",
              "currency": "EUR",
              "openingBalance": 500.00,
              "transactions": [
                { "id": "t1", "valueDate": "2026-01-05", "amount": 1200.00 },
                { "id": "t2", "valueDate": "2026-01-10", "amount": -345.50 }
              ]
            }
            """;

    private <T> ResponseEntity<T> postSummary(Class<T> bodyType) {
        return postSummary("acc-1", bodyType);
    }

    private <T> ResponseEntity<T> postSummary(String accountId, Class<T> bodyType) {
        return RestClient.create("http://localhost:" + port)
                .post()
                .uri("/summaries?accountId={accountId}&month=2026-01", accountId)
                .retrieve()
                .onStatus(status -> true, (request, response) -> {
                })
                .toEntity(bodyType);
    }

    @Test
    void calculatesPublishesAndReturnsTheSummary() {
        wireMock.stubFor(get("/accounts/acc-1/statements/2026-01").willReturn(okJson(STATEMENT)));
        wireMock.stubFor(post("/monthly-summaries").willReturn(created()));

        ResponseEntity<MonthlySummary> response = postSummary(MonthlySummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MonthlySummary summary = response.getBody();
        assertThat(summary.accountId()).isEqualTo("acc-1");
        assertThat(summary.month()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(summary.currency()).isEqualTo("EUR");
        assertThat(summary.totalIncome()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(summary.totalSpending()).isEqualByComparingTo(new BigDecimal("345.50"));
        assertThat(summary.monthlyBalance()).isEqualByComparingTo(new BigDecimal("854.50"));
        assertThat(summary.openingBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(summary.closingBalance()).isEqualByComparingTo(new BigDecimal("1354.50"));

        wireMock.verify(postRequestedFor(urlEqualTo("/monthly-summaries"))
                .withRequestBody(equalToJson("""
                        {
                          "accountId": "acc-1",
                          "month": "2026-01",
                          "currency": "EUR",
                          "totalIncome": 1200.00,
                          "totalSpending": 345.50,
                          "monthlyBalance": 854.50,
                          "openingBalance": 500.00,
                          "closingBalance": 1354.50
                        }
                        """)));
    }

    @Test
    void returns404WhenTheStatementsApiHasNoStatement() {
        wireMock.stubFor(get("/accounts/acc-1/statements/2026-01").willReturn(notFound()));

        ResponseEntity<ErrorResponse> response = postSummary(ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("No statement found");
        wireMock.verify(0, postRequestedFor(urlEqualTo("/monthly-summaries")));
    }

    @Test
    void returns502WhenTheSummaryApiFails() {
        wireMock.stubFor(get("/accounts/acc-1/statements/2026-01").willReturn(okJson(STATEMENT)));
        wireMock.stubFor(post("/monthly-summaries").willReturn(serverError()));

        ResponseEntity<ErrorResponse> response = postSummary(ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().message()).isEqualTo("Summary API failed");
    }

    @Test
    void neverLogsAccountIdOnStatementsTimeout(CapturedOutput output) {
        wireMock.stubFor(get("/accounts/" + SECRET_ACCOUNT + "/statements/2026-01")
                .willReturn(okJson(STATEMENT).withFixedDelay(1500)));

        assertThat(postSummary(SECRET_ACCOUNT, ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertAccountIdNotLogged(output);
    }

    @Test
    void neverLogsAccountIdOnStatementsServerError(CapturedOutput output) {
        wireMock.stubFor(get("/accounts/" + SECRET_ACCOUNT + "/statements/2026-01")
                .willReturn(aResponse().withStatus(500).withBody("database down for " + SECRET_ACCOUNT)));

        assertThat(postSummary(SECRET_ACCOUNT, ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertAccountIdNotLogged(output);
    }

    @Test
    void neverLogsAccountIdOnStatementNotFound(CapturedOutput output) {
        wireMock.stubFor(get("/accounts/" + SECRET_ACCOUNT + "/statements/2026-01").willReturn(notFound()));

        assertThat(postSummary(SECRET_ACCOUNT, ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertAccountIdNotLogged(output);
    }

    @Test
    void neverLogsAccountIdOnSummaryApiError(CapturedOutput output) {
        wireMock.stubFor(get("/accounts/" + SECRET_ACCOUNT + "/statements/2026-01")
                .willReturn(okJson(STATEMENT.replace("acc-1", SECRET_ACCOUNT))));
        wireMock.stubFor(post("/monthly-summaries")
                .willReturn(aResponse().withStatus(500).withBody("cannot store summary for " + SECRET_ACCOUNT)));

        assertThat(postSummary(SECRET_ACCOUNT, ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertAccountIdNotLogged(output);
    }

    private void assertAccountIdNotLogged(CapturedOutput output) {
        assertThat(output.getAll()).contains("Request failed").doesNotContain(SECRET_ACCOUNT);
    }
}
