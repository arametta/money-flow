package com.example.moneyflow.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/** One bank statement for one account and month. */
public record Statement(
        String accountId,
        YearMonth month,
        String currency,
        BigDecimal openingBalance,
        List<Transaction> transactions) {

    public Statement {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(openingBalance, "openingBalance");
        transactions = List.copyOf(Objects.requireNonNull(transactions, "transactions"));
    }
}
