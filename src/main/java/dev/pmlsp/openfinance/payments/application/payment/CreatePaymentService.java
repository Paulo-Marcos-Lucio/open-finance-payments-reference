package dev.pmlsp.openfinance.payments.application.payment;

import dev.pmlsp.openfinance.payments.domain.exception.ConsentNotAuthorisedException;
import dev.pmlsp.openfinance.payments.domain.exception.ConsentNotFoundException;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;
import dev.pmlsp.openfinance.payments.domain.port.in.CreatePaymentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent.AuditKind;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditLog;
import dev.pmlsp.openfinance.payments.domain.port.out.Clock;
import dev.pmlsp.openfinance.payments.domain.port.out.ConsentRepository;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderPaymentGateway;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderPaymentGateway.PaymentSettlementResult;
import dev.pmlsp.openfinance.payments.domain.port.out.PaymentRepository;

import java.time.Instant;

public class CreatePaymentService implements CreatePaymentUseCase {

    private final ConsentRepository consentRepository;
    private final PaymentRepository paymentRepository;
    private final HolderPaymentGateway holderGateway;
    private final AuditLog auditLog;
    private final Clock clock;

    public CreatePaymentService(ConsentRepository consentRepository,
                                PaymentRepository paymentRepository,
                                HolderPaymentGateway holderGateway,
                                AuditLog auditLog,
                                Clock clock) {
        this.consentRepository = consentRepository;
        this.paymentRepository = paymentRepository;
        this.holderGateway = holderGateway;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public PaymentInitiation create(CreatePaymentCommand command) {
        Instant now = clock.now();
        Consent consent = consentRepository.findById(command.consentId())
                .orElseThrow(() -> new ConsentNotFoundException(command.consentId().value()));
        if (!consent.isAuthorised()) {
            throw new ConsentNotAuthorisedException(consent.id().value(), consent.status().name());
        }

        PaymentInitiation pending = PaymentInitiation.newReceived(
                consent.id(), consent.creditor(), command.debtor(),
                consent.amount(), command.debtor().ispb(), now)
                .toPending(now);
        paymentRepository.save(pending);
        auditLog.emit(new AuditEvent(now, AuditKind.PAYMENT_PENDING, pending.id().value(),
                "ok", "consent=" + consent.id().value()));

        PaymentSettlementResult result = holderGateway.submit(pending);
        Instant after = clock.now();

        PaymentInitiation outcome;
        if (result.accepted()) {
            outcome = pending.accept(after);
            auditLog.emit(new AuditEvent(after, AuditKind.PAYMENT_ACCEPTED, pending.id().value(),
                    "ok", null));
            Consent consumed = consent.consume(after);
            consentRepository.save(consumed);
            auditLog.emit(new AuditEvent(after, AuditKind.CONSENT_CONSUMED,
                    consumed.id().value(), "ok", null));
        } else {
            outcome = pending.reject(result.rejectionReason(), after);
            auditLog.emit(new AuditEvent(after, AuditKind.PAYMENT_REJECTED, pending.id().value(),
                    "rejected", result.rejectionReason()));
        }
        paymentRepository.save(outcome);
        return outcome;
    }
}
