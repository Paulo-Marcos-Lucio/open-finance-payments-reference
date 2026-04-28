package dev.pmlsp.openfinance.payments.application.payment;

import dev.pmlsp.openfinance.payments.domain.exception.PaymentNotFoundException;
import dev.pmlsp.openfinance.payments.domain.model.PaymentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;
import dev.pmlsp.openfinance.payments.domain.port.in.SettlePaymentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent.AuditKind;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditLog;
import dev.pmlsp.openfinance.payments.domain.port.out.Clock;
import dev.pmlsp.openfinance.payments.domain.port.out.PaymentRepository;

public class SettlePaymentService implements SettlePaymentUseCase {

    private final PaymentRepository repository;
    private final AuditLog auditLog;
    private final Clock clock;

    public SettlePaymentService(PaymentRepository repository, AuditLog auditLog, Clock clock) {
        this.repository = repository;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public PaymentInitiation settle(PaymentId id) {
        PaymentInitiation payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id.value()));
        PaymentInitiation settled = payment.settle(clock.now());
        repository.save(settled);
        auditLog.emit(new AuditEvent(clock.now(), AuditKind.PAYMENT_SETTLED,
                settled.id().value(), "ok",
                "endToEndId=" + settled.endToEndId().value()));
        return settled;
    }
}
