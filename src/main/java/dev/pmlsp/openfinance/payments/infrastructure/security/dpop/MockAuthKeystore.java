package dev.pmlsp.openfinance.payments.infrastructure.security.dpop;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-process key material that mimics the authorization server's signing key.
 * Generates a fresh RSA key on startup. Used both by {@link TrustedJwkSetProvider}
 * (resource-server side) and by the mock token issuer endpoint.
 * <p>
 * <strong>Not for production.</strong> A real deployment uses an HSM-backed key
 * exposed via the auth server's JWKS endpoint and rotated per the BCB profile.
 */
@Component
public class MockAuthKeystore {

    private final RSAKey signingKey;

    public MockAuthKeystore() {
        try {
            this.signingKey = new RSAKeyGenerator(2048)
                    .keyID("mock-auth-" + UUID.randomUUID().toString().substring(0, 8))
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("could not generate mock auth signing key", e);
        }
    }

    public JWKSet publicJwkSet() {
        return new JWKSet(List.of(signingKey.toPublicJWK()));
    }

    public String issueAccessToken(String clientId, String subject, String scope,
                                   String dpopThumbprint, Duration validity) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("mock-auth")
                    .subject(subject)
                    .claim("client_id", clientId)
                    .claim("scope", scope)
                    .issueTime(java.util.Date.from(now))
                    .expirationTime(java.util.Date.from(now.plus(validity)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("cnf", Map.of("jkt", dpopThumbprint))
                    .build();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(signingKey.getKeyID())
                    .type(com.nimbusds.jose.JOSEObjectType.JWT)
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            JWSSigner signer = new RSASSASigner(signingKey);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("could not sign access token", e);
        }
    }
}
