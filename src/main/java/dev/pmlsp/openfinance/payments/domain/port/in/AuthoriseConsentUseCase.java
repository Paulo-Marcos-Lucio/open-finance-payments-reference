package dev.pmlsp.openfinance.payments.domain.port.in;

import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;

public interface AuthoriseConsentUseCase {
    Consent authorise(ConsentId id);
}
