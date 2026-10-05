package com.example.moneyflow.client;

/** Hides most of an account id so it can be written to logs safely. */
final class AccountIds {

    private AccountIds() {
    }

    static String mask(String accountId) {
        return accountId.length() <= 4 ? "****" : accountId.substring(accountId.length() - 4);
    }

    static String causeType(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getClass().getSimpleName();
    }
}
