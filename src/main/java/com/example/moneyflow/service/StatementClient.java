package com.example.moneyflow.service;

import com.example.moneyflow.model.Statement;

import java.time.YearMonth;

public interface StatementClient {

    Statement getStatement(String accountId, YearMonth month);
}
