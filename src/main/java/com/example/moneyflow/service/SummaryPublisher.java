package com.example.moneyflow.service;

import com.example.moneyflow.model.MonthlySummary;

/** Sends a monthly summary to the external summary API. */
public interface SummaryPublisher {

    void publish(MonthlySummary summary);
}
