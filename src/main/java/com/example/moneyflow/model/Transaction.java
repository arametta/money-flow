package com.example.moneyflow.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One transaction on a statement. */
public record Transaction(String id, LocalDate valueDate, BigDecimal amount) {
}
