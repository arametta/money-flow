package com.example.moneyflow.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** One bank statement for one account and month. */
public record Statement(
        String accountId,
        YearMonth month,
        String currency,
        BigDecimal openingBalance,
        List<Transaction> transactions) {
}
