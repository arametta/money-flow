package com.example.moneyflow.client;

import com.example.moneyflow.error.SummaryUnavailableException;
import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.service.SummaryPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static com.example.moneyflow.client.AccountIds.causeType;
import static com.example.moneyflow.client.AccountIds.mask;

/** Sends the summary to the real summary API over HTTP. */
@Component
public class HttpSummaryPublisher implements SummaryPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpSummaryPublisher.class);

    private final RestClient restClient;

    public HttpSummaryPublisher(SummaryApiProperties properties) {
        this.restClient = RestClients.create(properties.baseUrl(), properties.timeout());
    }

    @Override
    public void publish(MonthlySummary summary) {
        try {
            restClient.post()
                    .uri("/monthly-summaries")
                    .body(summary)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Summary API failed for account ending in {} ({})", mask(summary.accountId()), causeType(e));
            throw new SummaryUnavailableException("Summary API failed", null);
        }
    }
}
