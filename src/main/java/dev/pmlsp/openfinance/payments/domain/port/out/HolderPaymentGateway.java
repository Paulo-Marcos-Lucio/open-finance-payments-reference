package dev.pmlsp.openfinance.payments.domain.port.out;

import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;

public interface HolderPaymentGateway {
    PaymentSettlementResult submit(PaymentInitiation payment);

    record PaymentSettlementResult(boolean accepted, String rejectionReason) {}
}
