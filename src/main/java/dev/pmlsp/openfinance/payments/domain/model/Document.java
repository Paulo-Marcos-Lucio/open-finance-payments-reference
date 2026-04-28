package dev.pmlsp.openfinance.payments.domain.model;

import java.util.Objects;

public record Document(DocumentType type, String value) {
    public Document {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        int len = value.length();
        switch (type) {
            case CPF -> {
                if (len != 11) {
                    throw new IllegalArgumentException("CPF must have 11 digits, got " + len);
                }
            }
            case CNPJ -> {
                if (len != 14) {
                    throw new IllegalArgumentException("CNPJ must have 14 digits, got " + len);
                }
            }
        }
    }

    public String masked() {
        if (value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    public enum DocumentType { CPF, CNPJ }
}
