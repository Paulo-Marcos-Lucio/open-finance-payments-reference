package dev.pmlsp.openfinance.payments.application.consent;

import dev.pmlsp.openfinance.payments.domain.exception.ConsentNotFoundException;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.port.in.GetConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.out.Clock;
import dev.pmlsp.openfinance.payments.domain.port.out.ConsentRepository;

public class GetConsentService implements GetConsentUseCase {

    private final ConsentRepository repository;
    private final Clock clock;

    public GetConsentService(ConsentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Consent get(ConsentId id) {
        Consent consent = repository.findById(id)
                .orElseThrow(() -> new ConsentNotFoundException(id.value()));
        Consent maybeExpired = consent.expireIfNeeded(clock.now());
        if (maybeExpired != consent) {
            repository.save(maybeExpired);
        }
        return maybeExpired;
    }
}
