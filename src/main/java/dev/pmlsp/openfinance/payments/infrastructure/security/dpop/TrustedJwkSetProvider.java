package dev.pmlsp.openfinance.payments.infrastructure.security.dpop;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.stereotype.Component;

import java.text.ParseException;

/**
 * Holds the JWK set trusted to sign access tokens (i.e. the authorization server's signing keys).
 * <p>
 * In production, this would be loaded from {@code <auth-server>/.well-known/jwks.json} and
 * cached with TTL. For v0.2.0 reference, the keys are produced in-process by the
 * {@link MockAuthKeystore} so the IT can run end-to-end without a real authorization server.
 */
@Component
public class TrustedJwkSetProvider {

    private final MockAuthKeystore keystore;

    public TrustedJwkSetProvider(MockAuthKeystore keystore) {
        this.keystore = keystore;
    }

    public JWKSet jwkSet() {
        try {
            return JWKSet.parse(keystore.publicJwkSet().toString());
        } catch (ParseException e) {
            throw new IllegalStateException("could not load trusted JWK set", e);
        }
    }
}
