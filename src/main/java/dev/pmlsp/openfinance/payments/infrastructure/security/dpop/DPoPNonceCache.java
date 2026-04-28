package dev.pmlsp.openfinance.payments.infrastructure.security.dpop;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Anti-replay cache for DPoP proof <code>jti</code> identifiers.
 * Holds each <code>jti</code> for the proof's lifetime window so the same proof JWT
 * cannot be reused.
 */
@Component
public class DPoPNonceCache {

    private final Cache<String, Boolean> seen = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .build();

    /**
     * @return true if this <code>jti</code> was not seen yet (and is now recorded), false if already seen.
     */
    public boolean recordIfAbsent(String jti) {
        if (seen.getIfPresent(jti) != null) {
            return false;
        }
        seen.put(jti, Boolean.TRUE);
        return true;
    }
}
