package dev.pmlsp.openfinance.payments.domain.model;

import java.util.Objects;

public record Subject(SubjectType type, Document document, String name) {
    public Subject {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(document, "document");
    }

    public String maskedDocument() {
        return document.masked();
    }

    public enum SubjectType { LOGGED_USER, BUSINESS_ENTITY }
}
