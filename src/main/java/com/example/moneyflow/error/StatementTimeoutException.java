package com.example.moneyflow.error;

/** The statements API did not respond in time. */
public class StatementTimeoutException extends RuntimeException {

    public StatementTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
