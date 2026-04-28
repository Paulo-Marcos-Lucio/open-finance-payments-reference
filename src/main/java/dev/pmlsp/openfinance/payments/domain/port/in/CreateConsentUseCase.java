package dev.pmlsp.openfinance.payments.domain.port.in;

import dev.pmlsp.openfinance.payments.domain.model.Account;
import dev.pmlsp.openfinance.payments.domain.model.Amount;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.Subject;

public interface CreateConsentUseCase {

    Consent create(CreateConsentCommand command);

    record CreateConsentCommand(
            Subject loggedUser,
            Subject businessEntity,
            Account creditor,
            Amount amount,
            Integer validityMinutes
    ) {}
}
