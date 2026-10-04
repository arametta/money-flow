package com.example.moneyflow.client;

import com.example.moneyflow.error.SummaryUnavailableException;
import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.service.SummaryPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Sends the summary to the real summary API over HTTP. */
@Component
public class HttpSummaryPublisher implements SummaryPublisher {

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
            throw new SummaryUnavailableException("Summary API failed", e);
        }
    }
}
