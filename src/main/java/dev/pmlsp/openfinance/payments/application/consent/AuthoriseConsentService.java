package dev.pmlsp.openfinance.payments.application.consent;

import dev.pmlsp.openfinance.payments.domain.exception.ConsentNotFoundException;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.port.in.AuthoriseConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent.AuditKind;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditLog;
import dev.pmlsp.openfinance.payments.domain.port.out.Clock;
import dev.pmlsp.openfinance.payments.domain.port.out.ConsentRepository;

public class AuthoriseConsentService implements AuthoriseConsentUseCase {

    private final ConsentRepository repository;
    private final AuditLog auditLog;
    private final Clock clock;

    public AuthoriseConsentService(ConsentRepository repository, AuditLog auditLog, Clock clock) {
        this.repository = repository;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public Consent authorise(ConsentId id) {
        Consent consent = repository.findById(id)
                .orElseThrow(() -> new ConsentNotFoundException(id.value()));
        Consent authorised = consent.authorise(clock.now());
        repository.save(authorised);
        auditLog.emit(new AuditEvent(clock.now(), AuditKind.CONSENT_AUTHORISED,
                authorised.id().value(), "ok", null));
        return authorised;
    }
}
