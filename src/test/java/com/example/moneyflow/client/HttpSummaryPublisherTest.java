package com.example.moneyflow.client;

import com.example.moneyflow.model.MonthlySummary;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.YearMonth;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpSummaryPublisherTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private static final MonthlySummary SUMMARY = new MonthlySummary(
            "acc-1",
            YearMonth.of(2026, 1),
            "EUR",
            new BigDecimal("100.00"),
            new BigDecimal("30.00"),
            new BigDecimal("70.00"),
            new BigDecimal("500.00"),
            new BigDecimal("570.00"));

    private HttpSummaryPublisher publisherWithTimeout(Duration timeout) {
        return new HttpSummaryPublisher(new SummaryApiProperties(wireMock.baseUrl(), timeout));
    }

    @Test
    void publishesSuccessfully() {
        wireMock.stubFor(post("/monthly-summaries").willReturn(created()));

        assertThatCode(() -> publisherWithTimeout(Duration.ofSeconds(5)).publish(SUMMARY))
                .doesNotThrowAnyException();
    }

    @Test
    void throwsNotFoundOn404() {
        wireMock.stubFor(post("/monthly-summaries").willReturn(notFound()));

        assertThatThrownBy(() -> publisherWithTimeout(Duration.ofSeconds(5)).publish(SUMMARY))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    void throwsServerErrorOn500() {
        wireMock.stubFor(post("/monthly-summaries").willReturn(serverError()));

        assertThatThrownBy(() -> publisherWithTimeout(Duration.ofSeconds(5)).publish(SUMMARY))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void throwsOnTimeout() {
        wireMock.stubFor(post("/monthly-summaries").willReturn(ok().withFixedDelay(500)));

        assertThatThrownBy(() -> publisherWithTimeout(Duration.ofMillis(100)).publish(SUMMARY))
                .isInstanceOf(ResourceAccessException.class);
    }
}
