package dev.pmlsp.openfinance.payments.domain.model;

import java.util.Objects;

public record Account(Ispb ispb, String issuer, String number, AccountType type) {
    public Account {
        Objects.requireNonNull(ispb, "ispb");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(type, "type");
        if (number.isBlank()) {
            throw new IllegalArgumentException("account number cannot be blank");
        }
    }

    public enum AccountType { CACC, SVGS, SLRY, TRAN }
}
