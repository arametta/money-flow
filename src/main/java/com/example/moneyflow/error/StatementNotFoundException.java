package com.example.moneyflow.error;

/** The statements API has no statement for this account and month. */
public class StatementNotFoundException extends RuntimeException {

    public StatementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
