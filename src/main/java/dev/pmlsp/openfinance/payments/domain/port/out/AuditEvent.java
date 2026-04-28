package dev.pmlsp.openfinance.payments.domain.port.out;

import java.time.Instant;

public record AuditEvent(
        Instant occurredAt,
        AuditKind kind,
        String entityId,
        String outcome,
        String details
) {

    public enum AuditKind {
        CONSENT_CREATED,
        CONSENT_AUTHORISED,
        CONSENT_REJECTED,
        CONSENT_REVOKED,
        CONSENT_EXPIRED,
        CONSENT_CONSUMED,
        PAYMENT_CREATED,
        PAYMENT_PENDING,
        PAYMENT_ACCEPTED,
        PAYMENT_SETTLED,
        PAYMENT_REJECTED,
        HOLDER_ERROR
    }
}
