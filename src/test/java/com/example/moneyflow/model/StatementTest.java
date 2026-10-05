package com.example.moneyflow.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Checks that a statement can only be created with all its required fields. */
class StatementTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 1);
    private static final BigDecimal BALANCE = new BigDecimal("100.00");
    private static final Transaction TRANSACTION =
            new Transaction("t1", LocalDate.of(2026, 1, 5), new BigDecimal("50.00"));

    @Test
    void rejectsMissingFields() {
        assertThatThrownBy(() -> new Statement(null, MONTH, "EUR", BALANCE, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("accountId");
        assertThatThrownBy(() -> new Statement("acc-1", null, "EUR", BALANCE, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("month");
        assertThatThrownBy(() -> new Statement("acc-1", MONTH, null, BALANCE, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("currency");
        assertThatThrownBy(() -> new Statement("acc-1", MONTH, "EUR", null, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("openingBalance");
        assertThatThrownBy(() -> new Statement("acc-1", MONTH, "EUR", BALANCE, null))
                .isInstanceOf(NullPointerException.class).hasMessage("transactions");
    }

    @Test
    void rejectsMissingTransactionInTheList() {
        List<Transaction> withGap = Arrays.asList(TRANSACTION, null);

        assertThatThrownBy(() -> new Statement("acc-1", MONTH, "EUR", BALANCE, withGap))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void keepsItsOwnUnchangeableCopyOfTheTransactions() {
        List<Transaction> original = new ArrayList<>(List.of(TRANSACTION));
        Statement statement = new Statement("acc-1", MONTH, "EUR", BALANCE, original);

        original.clear();

        assertThat(statement.transactions()).containsExactly(TRANSACTION);
        assertThatThrownBy(() -> statement.transactions().add(TRANSACTION))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
