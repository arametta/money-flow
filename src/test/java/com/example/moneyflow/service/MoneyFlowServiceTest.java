package com.example.moneyflow.service;

import com.example.moneyflow.error.StatementUnavailableException;
import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.model.Statement;
import com.example.moneyflow.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyFlowServiceTest {

    @Test
    void fetchesCalculatesAndPublishesTheSummary() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                new BigDecimal("100.00"),
                List.of(
                        new Transaction("t1", LocalDate.of(2026, 1, 5), new BigDecimal("50.00")),
                        new Transaction("t2", LocalDate.of(2026, 1, 10), new BigDecimal("-20.00"))));
        FakeStatementClient statementClient = new FakeStatementClient(statement);
        FakeSummaryPublisher summaryPublisher = new FakeSummaryPublisher();
        MoneyFlowService service = new MoneyFlowService(
                statementClient, new StatementCalculator(), summaryPublisher);

        MonthlySummary result = service.process("acc-1", YearMonth.of(2026, 1));

        assertThat(statementClient.requestedAccountId).isEqualTo("acc-1");
        assertThat(statementClient.requestedMonth).isEqualTo(YearMonth.of(2026, 1));
        assertThat(result.totalIncome()).isEqualByComparingTo("50.00");
        assertThat(result.totalSpending()).isEqualByComparingTo("20.00");
        assertThat(result.closingBalance()).isEqualByComparingTo("130.00");
        assertThat(summaryPublisher.published).isEqualTo(result);
    }

    @Test
    void throwsWhenStatementDoesNotMatchTheRequest() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                new BigDecimal("100.00"),
                List.of());
        FakeStatementClient statementClient = new FakeStatementClient(statement);
        FakeSummaryPublisher summaryPublisher = new FakeSummaryPublisher();
        MoneyFlowService service = new MoneyFlowService(
                statementClient, new StatementCalculator(), summaryPublisher);

        assertThatThrownBy(() -> service.process("acc-999", YearMonth.of(2025, 7)))
                .isInstanceOf(StatementUnavailableException.class);
        assertThat(summaryPublisher.published).isNull();
    }

    private static class FakeStatementClient implements StatementClient {
        private final Statement statement;
        private String requestedAccountId;
        private YearMonth requestedMonth;

        FakeStatementClient(Statement statement) {
            this.statement = statement;
        }

        @Override
        public Statement getStatement(String accountId, YearMonth month) {
            this.requestedAccountId = accountId;
            this.requestedMonth = month;
            return statement;
        }
    }

    private static class FakeSummaryPublisher implements SummaryPublisher {
        private MonthlySummary published;

        @Override
        public void publish(MonthlySummary summary) {
            this.published = summary;
        }
    }
}
