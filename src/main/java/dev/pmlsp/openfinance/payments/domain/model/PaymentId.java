package dev.pmlsp.openfinance.payments.domain.model;

import java.util.Objects;

public record PaymentId(String value) {
    public PaymentId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("payment id cannot be blank");
        }
    }
}
