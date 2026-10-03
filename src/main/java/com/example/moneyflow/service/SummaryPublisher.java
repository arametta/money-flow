package com.example.moneyflow.service;

import com.example.moneyflow.model.MonthlySummary;

public interface SummaryPublisher {

    void publish(MonthlySummary summary);
}
