package dev.pmlsp.openfinance.payments.domain.port.out;

import dev.pmlsp.openfinance.payments.domain.model.PaymentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;

import java.util.Optional;

public interface PaymentRepository {
    void save(PaymentInitiation payment);
    Optional<PaymentInitiation> findById(PaymentId id);
}
