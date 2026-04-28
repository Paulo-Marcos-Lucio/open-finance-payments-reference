package dev.pmlsp.openfinance.payments.infrastructure.persistence;

import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.port.out.ConsentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryConsentRepository implements ConsentRepository {

    private final ConcurrentMap<String, Consent> store = new ConcurrentHashMap<>();

    @Override
    public void save(Consent consent) {
        store.put(consent.id().value(), consent);
    }

    @Override
    public Optional<Consent> findById(ConsentId id) {
        return Optional.ofNullable(store.get(id.value()));
    }
}
