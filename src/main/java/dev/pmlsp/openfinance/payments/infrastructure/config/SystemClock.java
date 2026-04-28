package dev.pmlsp.openfinance.payments.infrastructure.config;

import dev.pmlsp.openfinance.payments.domain.port.out.Clock;

import java.time.Instant;

public class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
