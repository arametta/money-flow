package com.example.moneyflow.client;

import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.service.SummaryPublisher;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Sends the summary to the real summary API over HTTP. */
@Component
public class HttpSummaryPublisher implements SummaryPublisher {

    private final RestClient restClient;

    public HttpSummaryPublisher(SummaryApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void publish(MonthlySummary summary) {
        restClient.post()
                .uri("/monthly-summaries")
                .body(summary)
                .retrieve()
                .toBodilessEntity();
    }
}
