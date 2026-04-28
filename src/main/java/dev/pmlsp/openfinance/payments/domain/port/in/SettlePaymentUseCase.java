package dev.pmlsp.openfinance.payments.domain.port.in;

import dev.pmlsp.openfinance.payments.domain.model.PaymentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;

public interface SettlePaymentUseCase {
    PaymentInitiation settle(PaymentId id);
}
