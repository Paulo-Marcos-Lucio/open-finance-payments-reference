package dev.pmlsp.openfinance.payments.domain.model;

import java.util.Objects;

public record ConsentId(String value) {
    public ConsentId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("consent id cannot be blank");
        }
        if (value.length() > 256) {
            throw new IllegalArgumentException("consent id too long: " + value.length());
        }
    }
}
