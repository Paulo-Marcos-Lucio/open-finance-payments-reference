package dev.pmlsp.openfinance.payments.application.payment;

import dev.pmlsp.openfinance.payments.domain.exception.PaymentNotFoundException;
import dev.pmlsp.openfinance.payments.domain.model.PaymentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;
import dev.pmlsp.openfinance.payments.domain.port.in.GetPaymentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.out.PaymentRepository;

public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepository repository;

    public GetPaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentInitiation get(PaymentId id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id.value()));
    }
}
