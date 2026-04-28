package dev.pmlsp.openfinance.payments.infrastructure.security.dpop;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.Map;

/**
 * Introspects access tokens issued by the trusted authorization server.
 * <p>
 * Supports two formats:
 * <ul>
 *   <li><strong>JWT (RS256)</strong> — verified against the configured trusted JWK set.
 *       Must carry <code>cnf.jkt</code> for sender-constrained binding (DPoP).</li>
 *   <li>Reserved for future opaque introspection — not implemented in v0.2.0.</li>
 * </ul>
 */
@Component
public class AccessTokenIntrospector {

    private final JWKSet trustedKeys;

    public AccessTokenIntrospector(TrustedJwkSetProvider provider) {
        this.trustedKeys = provider.jwkSet();
    }

    public IntrospectedToken introspect(String accessToken) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(accessToken);
        } catch (ParseException e) {
            throw new DPoPProofException("invalid_token", "access token not parseable");
        }

        if (jwt.getHeader().getAlgorithm() != JWSAlgorithm.RS256) {
            throw new DPoPProofException("invalid_token",
                    "unsupported access token alg: " + jwt.getHeader().getAlgorithm());
        }

        String kid = jwt.getHeader().getKeyID();
        var key = trustedKeys.getKeyByKeyId(kid);
        if (!(key instanceof RSAKey rsa)) {
            throw new DPoPProofException("invalid_token", "no trusted JWK with kid=" + kid);
        }

        try {
            JWSVerifier verifier = new RSASSAVerifier(rsa.toRSAPublicKey());
            if (!jwt.verify(verifier)) {
                throw new DPoPProofException("invalid_token", "access token signature invalid");
            }
        } catch (JOSEException e) {
            throw new DPoPProofException("invalid_token",
                    "access token signature verification failed: " + e.getMessage());
        }

        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new DPoPProofException("invalid_token", "access token claims not readable");
        }

        var exp = claims.getExpirationTime();
        if (exp == null || Instant.now().isAfter(exp.toInstant())) {
            throw new DPoPProofException("invalid_token", "access token expired");
        }

        Object cnf = claims.getClaim("cnf");
        String jkt = null;
        if (cnf instanceof Map<?, ?> map && map.get("jkt") instanceof String s) {
            jkt = s;
        }
        if (jkt == null) {
            throw new DPoPProofException("invalid_token",
                    "access token missing cnf.jkt — sender-constrained binding required");
        }

        String clientId = claims.getClaim("client_id") instanceof String s ? s : null;
        String scope = claims.getClaim("scope") instanceof String s ? s : null;
        return new IntrospectedToken(claims.getSubject(), clientId, scope, exp.toInstant(), jkt);
    }

    public record IntrospectedToken(String subject, String clientId, String scope, Instant exp, String cnfJkt) {}
}
