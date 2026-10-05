package com.example.moneyflow.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Checks that a transaction can only be created with all its required fields. */
class TransactionTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 5);
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");

    @Test
    void rejectsMissingFields() {
        assertThatThrownBy(() -> new Transaction(null, DATE, AMOUNT))
                .isInstanceOf(NullPointerException.class).hasMessage("id");
        assertThatThrownBy(() -> new Transaction("t1", null, AMOUNT))
                .isInstanceOf(NullPointerException.class).hasMessage("valueDate");
        assertThatThrownBy(() -> new Transaction("t1", DATE, null))
                .isInstanceOf(NullPointerException.class).hasMessage("amount");
    }
}
