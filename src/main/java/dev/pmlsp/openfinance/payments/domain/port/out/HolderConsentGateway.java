package dev.pmlsp.openfinance.payments.domain.port.out;

import dev.pmlsp.openfinance.payments.domain.model.Consent;

public interface HolderConsentGateway {
    void registerConsent(Consent consent);
    String authorisationUrl(Consent consent);
}
