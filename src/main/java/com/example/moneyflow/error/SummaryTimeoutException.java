package com.example.moneyflow.error;

/** The summary API did not respond in time, so the summary may or may not have been stored. */
public class SummaryTimeoutException extends RuntimeException {

    public SummaryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
