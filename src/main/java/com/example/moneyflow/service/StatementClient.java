package com.example.moneyflow.service;

import com.example.moneyflow.model.Statement;

import java.time.YearMonth;

/** Gets a statement from the external statements API. */
public interface StatementClient {

    Statement getStatement(String accountId, YearMonth month);
}
