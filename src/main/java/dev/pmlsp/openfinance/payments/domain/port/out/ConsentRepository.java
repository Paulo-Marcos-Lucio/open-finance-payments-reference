package dev.pmlsp.openfinance.payments.domain.port.out;

import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;

import java.util.Optional;

public interface ConsentRepository {
    void save(Consent consent);
    Optional<Consent> findById(ConsentId id);
}
