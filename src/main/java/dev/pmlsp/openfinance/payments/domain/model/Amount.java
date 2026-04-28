package dev.pmlsp.openfinance.payments.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Amount(BigDecimal value, String currency) {
    public Amount {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(currency, "currency");
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO 4217 3-letter code, got: " + currency);
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("amount cannot be negative: " + value);
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException("amount scale must be <= 2, got: " + value);
        }
    }

    public static Amount brl(String value) {
        return new Amount(new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY), "BRL");
    }

    public String asString() {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
