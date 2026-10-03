package com.example.moneyflow.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** The totals for one month, sent to the summary API. */
public record MonthlySummary(
        String accountId,
        YearMonth month,
        String currency,
        BigDecimal totalIncome,
        BigDecimal totalSpending,
        BigDecimal monthlyBalance,
        BigDecimal openingBalance,
        BigDecimal closingBalance) {
}
