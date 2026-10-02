package com.example.moneyflow.service;

import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.model.Statement;
import com.example.moneyflow.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatementCalculatorTest {

    private final StatementCalculator calculator = new StatementCalculator();

    @Test
    void sumsOnlyIncomeWhenAllAmountsArePositive() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                BigDecimal.ZERO,
                List.of(
                        new Transaction("t1", LocalDate.of(2026, 1, 5), new BigDecimal("100.00")),
                        new Transaction("t2", LocalDate.of(2026, 1, 10), new BigDecimal("50.00"))));

        MonthlySummary summary = calculator.calculate(statement);

        assertThat(summary.totalIncome()).isEqualByComparingTo("150.00");
        assertThat(summary.totalSpending()).isEqualByComparingTo("0.00");
    }

    @Test
    void sumsOnlySpendingWhenAllAmountsAreNegative() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                BigDecimal.ZERO,
                List.of(
                        new Transaction("t1", LocalDate.of(2026, 1, 5), new BigDecimal("-30.00")),
                        new Transaction("t2", LocalDate.of(2026, 1, 10), new BigDecimal("-20.00"))));

        MonthlySummary summary = calculator.calculate(statement);

        assertThat(summary.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(summary.totalSpending()).isEqualByComparingTo("50.00");
    }

    @Test
    void sumsIncomeAndSpendingSeparatelyWhenMixed() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                BigDecimal.ZERO,
                List.of(
                        new Transaction("t1", LocalDate.of(2026, 1, 5), new BigDecimal("100.00")),
                        new Transaction("t2", LocalDate.of(2026, 1, 10), new BigDecimal("-30.00"))));

        MonthlySummary summary = calculator.calculate(statement);

        assertThat(summary.totalIncome()).isEqualByComparingTo("100.00");
        assertThat(summary.totalSpending()).isEqualByComparingTo("30.00");
        assertThat(summary.monthlyBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void ignoresZeroAmountTransactions() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                BigDecimal.ZERO,
                List.of(
                        new Transaction("t1", LocalDate.of(2026, 1, 5), new BigDecimal("100.00")),
                        new Transaction("t2", LocalDate.of(2026, 1, 10), BigDecimal.ZERO)));

        MonthlySummary summary = calculator.calculate(statement);

        assertThat(summary.totalIncome()).isEqualByComparingTo("100.00");
        assertThat(summary.totalSpending()).isEqualByComparingTo("0.00");
    }

    @Test
    void returnsZeroTotalsWhenThereAreNoTransactions() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                new BigDecimal("200.00"),
                List.of());

        MonthlySummary summary = calculator.calculate(statement);

        assertThat(summary.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(summary.totalSpending()).isEqualByComparingTo("0.00");
        assertThat(summary.monthlyBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void addsMonthlyBalanceToOpeningBalanceForClosingBalance() {
        Statement statement = new Statement(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                new BigDecimal("500.00"),
                List.of(
                        new Transaction("t1", LocalDate.of(2026, 1, 5), new BigDecimal("100.00")),
                        new Transaction("t2", LocalDate.of(2026, 1, 10), new BigDecimal("-30.00"))));

        MonthlySummary summary = calculator.calculate(statement);

        assertThat(summary.closingBalance()).isEqualByComparingTo("570.00");
    }
}
