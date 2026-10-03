package com.example.moneyflow.service;

import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.model.Statement;

import java.time.YearMonth;

/** Fetches a statement, calculates the summary, and sends it. */
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
        MonthlySummary summary = statementCalculator.calculate(statement);
        summaryPublisher.publish(summary);
        return summary;
    }
}
