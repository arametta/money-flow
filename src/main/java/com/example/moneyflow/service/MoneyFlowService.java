package com.example.moneyflow.service;

import com.example.moneyflow.error.StatementNotFoundException;
import com.example.moneyflow.error.StatementTimeoutException;
import com.example.moneyflow.error.StatementUnavailableException;
import com.example.moneyflow.error.SummaryUnavailableException;
import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.model.Statement;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.time.YearMonth;

/** Fetches a statement, calculates the summary, and sends it. */
@Service
public class MoneyFlowService {

    private final StatementClient statementClient;
    private final StatementCalculator statementCalculator;
    private final SummaryPublisher summaryPublisher;

    public MoneyFlowService(
            StatementClient statementClient,
            StatementCalculator statementCalculator,
            SummaryPublisher summaryPublisher) {
        this.statementClient = statementClient;
        this.statementCalculator = statementCalculator;
        this.summaryPublisher = summaryPublisher;
    }

    public MonthlySummary process(String accountId, YearMonth month) {
        Statement statement = fetchStatement(accountId, month);
        MonthlySummary summary = statementCalculator.calculate(statement);
        sendSummary(summary);
        return summary;
    }

    private Statement fetchStatement(String accountId, YearMonth month) {
        try {
            return statementClient.getStatement(accountId, month);
        } catch (HttpClientErrorException.NotFound e) {
            throw new StatementNotFoundException("No statement for account " + accountId, e);
        } catch (ResourceAccessException e) {
            throw new StatementTimeoutException("Statements API timed out", e);
        } catch (RestClientException e) {
            throw new StatementUnavailableException("Statements API failed", e);
        }
    }

    private void sendSummary(MonthlySummary summary) {
        try {
            summaryPublisher.publish(summary);
        } catch (RestClientException e) {
            throw new SummaryUnavailableException("Summary API failed", e);
        }
    }
}
