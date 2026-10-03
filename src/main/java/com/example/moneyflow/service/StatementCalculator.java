package com.example.moneyflow.service;

import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.model.Statement;
import com.example.moneyflow.model.Transaction;

import java.math.BigDecimal;

/** Turns a statement into income, spending and balance totals. */
public class StatementCalculator {

    public MonthlySummary calculate(Statement statement) {
        BigDecimal totalIncome = statement.transactions().stream()
                .map(Transaction::amount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpending = statement.transactions().stream()
                .map(Transaction::amount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .negate();
        BigDecimal monthlyBalance = totalIncome.subtract(totalSpending);
        BigDecimal closingBalance = statement.openingBalance().add(monthlyBalance);

        return new MonthlySummary(
                statement.accountId(),
                statement.month(),
                statement.currency(),
                totalIncome,
                totalSpending,
                monthlyBalance,
                statement.openingBalance(),
                closingBalance);
    }
}
