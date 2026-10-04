package com.example.moneyflow.client;

import com.example.moneyflow.error.StatementNotFoundException;
import com.example.moneyflow.error.StatementTimeoutException;
import com.example.moneyflow.error.StatementUnavailableException;
import com.example.moneyflow.model.Statement;
import com.example.moneyflow.model.Transaction;
import com.example.moneyflow.service.StatementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.time.YearMonth;

/** Calls the real statements API over HTTP. */
@Component
public class HttpStatementClient implements StatementClient {

    private static final Logger log = LoggerFactory.getLogger(HttpStatementClient.class);

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
        try {
            Statement statement = restClient.get()
                    .uri("/accounts/{accountId}/statements/{month}", accountId, month)
                    .retrieve()
                    .body(Statement.class);
            validate(statement);
            return statement;
        } catch (HttpClientErrorException.NotFound e) {
            log.info("No statement for account ending in {}", mask(accountId));
            throw new StatementNotFoundException("No statement found", e);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new StatementTimeoutException("Statements API timed out", e);
            }
            throw new StatementUnavailableException("Statements API failed", e);
        } catch (RestClientException e) {
            throw new StatementUnavailableException("Statements API failed", e);
        }
    }

    private static String mask(String accountId) {
        return accountId.length() <= 4 ? accountId : accountId.substring(accountId.length() - 4);
    }

    private void validate(Statement statement) {
        if (statement == null
                || statement.accountId() == null
                || statement.month() == null
                || statement.currency() == null
                || statement.openingBalance() == null
                || statement.transactions() == null) {
            throw new StatementUnavailableException("Statements API returned an incomplete statement", null);
        }
        for (Transaction transaction : statement.transactions()) {
            if (transaction == null
                    || transaction.id() == null
                    || transaction.valueDate() == null
                    || transaction.amount() == null) {
                throw new StatementUnavailableException("Statements API returned an incomplete transaction", null);
            }
        }
    }
}
