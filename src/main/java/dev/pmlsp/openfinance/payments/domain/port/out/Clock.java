package dev.pmlsp.openfinance.payments.domain.port.out;

import java.time.Instant;

public interface Clock {
    Instant now();
}
