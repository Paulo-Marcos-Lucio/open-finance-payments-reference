package dev.pmlsp.openfinance.payments.domain.model;

import java.util.Objects;

public record Ispb(String value) {
    public Ispb {
        Objects.requireNonNull(value, "value");
        if (value.length() != 8 || !value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("ISPB must be 8 digits, got: " + value);
        }
    }
}
