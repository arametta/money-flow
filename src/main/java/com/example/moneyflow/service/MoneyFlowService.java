package com.example.moneyflow.service;

import com.example.moneyflow.error.StatementUnavailableException;
import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.model.Statement;
import org.springframework.stereotype.Service;

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
        Statement statement = statementClient.getStatement(accountId, month);
        if (!statement.accountId().equals(accountId) || !statement.month().equals(month)) {
            throw new StatementUnavailableException("Statement does not match the request", null);
        }
        MonthlySummary summary = statementCalculator.calculate(statement);
        summaryPublisher.publish(summary);
        return summary;
    }
}
