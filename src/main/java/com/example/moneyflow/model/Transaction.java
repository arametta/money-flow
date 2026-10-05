package com.example.moneyflow.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** One transaction on a statement. */
public record Transaction(String id, LocalDate valueDate, BigDecimal amount) {

    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(valueDate, "valueDate");
        Objects.requireNonNull(amount, "amount");
    }
}
