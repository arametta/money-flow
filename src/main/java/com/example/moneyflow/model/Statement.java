package com.example.moneyflow.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record Statement(
        String accountId,
        YearMonth month,
        String currency,
        BigDecimal openingBalance,
        List<Transaction> transactions) {
}
