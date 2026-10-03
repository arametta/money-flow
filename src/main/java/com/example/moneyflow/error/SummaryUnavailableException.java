package com.example.moneyflow.error;

/** The summary API failed, so the summary was not sent. */
public class SummaryUnavailableException extends RuntimeException {

    public SummaryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
