package dev.pmlsp.openfinance.payments.application.consent;

import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.port.in.CreateConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent.AuditKind;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditLog;
import dev.pmlsp.openfinance.payments.domain.port.out.Clock;
import dev.pmlsp.openfinance.payments.domain.port.out.ConsentRepository;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderConsentGateway;

import java.time.Duration;
import java.time.Instant;

public class CreateConsentService implements CreateConsentUseCase {

    private final ConsentRepository repository;
    private final HolderConsentGateway holderGateway;
    private final AuditLog auditLog;
    private final Clock clock;
    private final Duration defaultValidity;
    private final Duration maxValidity;

    public CreateConsentService(ConsentRepository repository,
                                HolderConsentGateway holderGateway,
                                AuditLog auditLog,
                                Clock clock,
                                Duration defaultValidity,
                                Duration maxValidity) {
        this.repository = repository;
        this.holderGateway = holderGateway;
        this.auditLog = auditLog;
        this.clock = clock;
        this.defaultValidity = defaultValidity;
        this.maxValidity = maxValidity;
    }

    @Override
    public Consent create(CreateConsentCommand command) {
        Instant now = clock.now();
        Duration validity = resolveValidity(command.validityMinutes());
        Instant expiresAt = now.plus(validity);

        Consent consent = Consent.newReceived(
                command.loggedUser(),
                command.businessEntity(),
                command.creditor(),
                command.amount(),
                now, expiresAt);

        holderGateway.registerConsent(consent);
        repository.save(consent);

        auditLog.emit(new AuditEvent(now, AuditKind.CONSENT_CREATED,
                consent.id().value(), "ok",
                "creditorIspb=" + consent.creditor().ispb().value()
                        + " amount=" + consent.amount().asString()));
        return consent;
    }

    private Duration resolveValidity(Integer minutes) {
        if (minutes == null) {
            return defaultValidity;
        }
        Duration requested = Duration.ofMinutes(minutes);
        return requested.compareTo(maxValidity) > 0 ? maxValidity : requested;
    }
}
