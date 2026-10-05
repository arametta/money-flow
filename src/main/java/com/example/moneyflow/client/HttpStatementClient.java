package com.example.moneyflow.client;

import com.example.moneyflow.error.StatementNotFoundException;
import com.example.moneyflow.error.StatementTimeoutException;
import com.example.moneyflow.error.StatementUnavailableException;
import com.example.moneyflow.model.Statement;
import com.example.moneyflow.model.Transaction;
import com.example.moneyflow.service.StatementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.time.YearMonth;

import static com.example.moneyflow.client.AccountIds.causeType;
import static com.example.moneyflow.client.AccountIds.mask;

/** Calls the real statements API over HTTP. */
@Component
public class HttpStatementClient implements StatementClient {

    private static final Logger log = LoggerFactory.getLogger(HttpStatementClient.class);

    private final RestClient restClient;

    public HttpStatementClient(StatementsApiProperties properties) {
        this.restClient = RestClients.create(properties.baseUrl(), properties.timeout());
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
            throw new StatementNotFoundException("No statement found", null);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                log.warn("Statements API timed out for account ending in {}", mask(accountId));
                throw new StatementTimeoutException("Statements API timed out", null);
            }
            log.warn("Statements API failed for account ending in {} ({})", mask(accountId), causeType(e));
            throw new StatementUnavailableException("Statements API failed", null);
        } catch (RestClientException e) {
            log.warn("Statements API failed for account ending in {} ({})", mask(accountId), causeType(e));
            throw new StatementUnavailableException("Statements API failed", null);
        }
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
