package com.example.moneyflow.client;

import com.example.moneyflow.model.Statement;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.YearMonth;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpStatementClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private HttpStatementClient clientWithTimeout(Duration timeout) {
        return new HttpStatementClient(new StatementsApiProperties(wireMock.baseUrl(), timeout));
    }

    @Test
    void returnsStatementOnSuccess() {
        wireMock.stubFor(get("/accounts/acc-1/statements/2026-01")
                .willReturn(okJson("""
                        {
                          "accountId": "acc-1",
                          "month": "2026-01",
                          "currency": "EUR",
                          "openingBalance": 100.00,
                          "transactions": [
                            {"id": "t1", "valueDate": "2026-01-05", "amount": 50.00}
                          ]
                        }
                        """)));

        Statement statement = clientWithTimeout(Duration.ofSeconds(5))
                .getStatement("acc-1", YearMonth.of(2026, 1));

        assertThat(statement.accountId()).isEqualTo("acc-1");
        assertThat(statement.month()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(statement.transactions()).hasSize(1);
        assertThat(statement.transactions().get(0).id()).isEqualTo("t1");
    }

    @Test
    void throwsNotFoundOn404() {
        wireMock.stubFor(get("/accounts/acc-1/statements/2026-01").willReturn(notFound()));

        assertThatThrownBy(() ->
                clientWithTimeout(Duration.ofSeconds(5)).getStatement("acc-1", YearMonth.of(2026, 1)))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    void throwsServerErrorOn500() {
        wireMock.stubFor(get("/accounts/acc-1/statements/2026-01").willReturn(serverError()));

        assertThatThrownBy(() ->
                clientWithTimeout(Duration.ofSeconds(5)).getStatement("acc-1", YearMonth.of(2026, 1)))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void throwsOnTimeout() {
        wireMock.stubFor(get("/accounts/acc-1/statements/2026-01")
                .willReturn(okJson("{}").withFixedDelay(500)));

        assertThatThrownBy(() ->
                clientWithTimeout(Duration.ofMillis(100)).getStatement("acc-1", YearMonth.of(2026, 1)))
                .isInstanceOf(ResourceAccessException.class);
    }
}
