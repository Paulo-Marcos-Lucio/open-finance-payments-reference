package dev.pmlsp.openfinance.payments.infrastructure.persistence;

import dev.pmlsp.openfinance.payments.domain.model.PaymentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;
import dev.pmlsp.openfinance.payments.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentMap<String, PaymentInitiation> store = new ConcurrentHashMap<>();

    @Override
    public void save(PaymentInitiation payment) {
        store.put(payment.id().value(), payment);
    }

    @Override
    public Optional<PaymentInitiation> findById(PaymentId id) {
        return Optional.ofNullable(store.get(id.value()));
    }
}
