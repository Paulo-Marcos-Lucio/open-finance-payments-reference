package dev.pmlsp.openfinance.payments.domain.port.in;

import dev.pmlsp.openfinance.payments.domain.model.Account;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;

public interface CreatePaymentUseCase {

    PaymentInitiation create(CreatePaymentCommand command);

    record CreatePaymentCommand(
            ConsentId consentId,
            Account debtor
    ) {}
}
