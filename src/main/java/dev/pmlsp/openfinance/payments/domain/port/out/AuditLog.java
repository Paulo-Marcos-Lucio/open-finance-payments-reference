package dev.pmlsp.openfinance.payments.domain.port.out;

public interface AuditLog {
    void emit(AuditEvent event);
}
