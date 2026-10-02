package com.example.moneyflow.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction(String id, LocalDate valueDate, BigDecimal amount) {
}
