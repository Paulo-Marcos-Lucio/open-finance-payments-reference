package dev.pmlsp.openfinance.payments.domain.port.in;

import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;

public interface GetConsentUseCase {
    Consent get(ConsentId id);
}
