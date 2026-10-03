package com.example.moneyflow.client;

import com.example.moneyflow.model.Statement;
import com.example.moneyflow.service.StatementClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.YearMonth;

/** Calls the real statements API over HTTP. */
@Component
public class HttpStatementClient implements StatementClient {

    private final RestClient restClient;

    public HttpStatementClient(StatementsApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Statement getStatement(String accountId, YearMonth month) {
        return restClient.get()
                .uri("/accounts/{accountId}/statements/{month}", accountId, month)
                .retrieve()
                .body(Statement.class);
    }
}
