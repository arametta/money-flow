package com.example.moneyflow.error;

/** The statements API failed or returned something we could not use. */
public class StatementUnavailableException extends RuntimeException {

    public StatementUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
