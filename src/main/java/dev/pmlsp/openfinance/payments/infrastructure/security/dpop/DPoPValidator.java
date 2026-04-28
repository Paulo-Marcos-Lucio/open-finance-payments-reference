package dev.pmlsp.openfinance.payments.infrastructure.security.dpop;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Validates DPoP proof JWTs per RFC 9449.
 * <p>
 * Each proof must:
 * <ol>
 *   <li>Parse as a signed JWT with <code>typ=dpop+jwt</code> and a <code>jwk</code> header.</li>
 *   <li>Verify under the embedded public key.</li>
 *   <li>Carry <code>htm</code> matching the request method (case-insensitive).</li>
 *   <li>Carry <code>htu</code> matching the request URI (canonicalized — query/fragment stripped).</li>
 *   <li>Have <code>iat</code> within the configured freshness window.</li>
 *   <li>Have a unique <code>jti</code> not yet seen by {@link DPoPNonceCache}.</li>
 *   <li>If <code>expectedThumbprint</code> is non-null, the proof's JWK thumbprint must equal it
 *       (sender-constrained binding to the access token's <code>cnf.jkt</code> claim).</li>
 * </ol>
 */
@Component
public class DPoPValidator {

    private static final JOSEObjectType DPOP_TYP = new JOSEObjectType("dpop+jwt");
    private static final Duration FRESHNESS = Duration.ofSeconds(60);
    private static final Set<JWSAlgorithm> ALLOWED_ALGS = Set.of(
            JWSAlgorithm.ES256,
            JWSAlgorithm.RS256,
            JWSAlgorithm.PS256);

    private final DPoPNonceCache nonceCache;

    public DPoPValidator(DPoPNonceCache nonceCache) {
        this.nonceCache = nonceCache;
    }

    public DPoPProof validate(String proofJwt,
                              String httpMethod,
                              String httpUri,
                              String expectedThumbprint) {
        SignedJWT jwt = parse(proofJwt);
        JWSHeader header = jwt.getHeader();

        if (!DPOP_TYP.equals(header.getType())) {
            throw new DPoPProofException("invalid_dpop_proof",
                    "DPoP proof header typ must be 'dpop+jwt'");
        }
        if (!ALLOWED_ALGS.contains(header.getAlgorithm())) {
            throw new DPoPProofException("invalid_dpop_proof",
                    "unsupported DPoP signing algorithm: " + header.getAlgorithm());
        }
        JWK jwk = header.getJWK();
        if (jwk == null || jwk.isPrivate()) {
            throw new DPoPProofException("invalid_dpop_proof",
                    "DPoP proof header must include a public 'jwk'");
        }

        verifySignature(jwt, jwk);

        JWTClaimsSet claims = readClaims(jwt);
        verifyHtm(claims, httpMethod);
        verifyHtu(claims, httpUri);
        Instant iat = verifyIat(claims);
        String jti = verifyJti(claims);

        String thumbprint = computeThumbprint(jwk);
        if (expectedThumbprint != null && !expectedThumbprint.equals(thumbprint)) {
            throw new DPoPProofException("invalid_token",
                    "DPoP proof key thumbprint does not match access token cnf.jkt");
        }

        return new DPoPProof(httpMethod.toUpperCase(), httpUri, iat, jti, jwk.toPublicJWK(), thumbprint);
    }

    public String computeThumbprint(JWK jwk) {
        try {
            return jwk.toPublicJWK().computeThumbprint().toString();
        } catch (Exception e) {
            throw new DPoPProofException("invalid_dpop_proof",
                    "could not compute JWK thumbprint: " + e.getMessage());
        }
    }

    private SignedJWT parse(String proofJwt) {
        try {
            return SignedJWT.parse(proofJwt);
        } catch (ParseException e) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof not parseable: " + e.getMessage());
        }
    }

    private void verifySignature(SignedJWT jwt, JWK jwk) {
        try {
            JWSVerifier verifier;
            if (jwk instanceof ECKey ec) {
                verifier = new ECDSAVerifier(ec.toPublicJWK());
            } else if (jwk instanceof RSAKey rsa) {
                verifier = new RSASSAVerifier(rsa.toPublicJWK());
            } else {
                throw new DPoPProofException("invalid_dpop_proof",
                        "DPoP proof key type not supported: " + jwk.getKeyType());
            }
            if (!jwt.verify(verifier)) {
                throw new DPoPProofException("invalid_dpop_proof", "DPoP proof signature invalid");
            }
        } catch (DPoPProofException e) {
            throw e;
        } catch (Exception e) {
            throw new DPoPProofException("invalid_dpop_proof",
                    "DPoP proof signature verification failed: " + e.getMessage());
        }
    }

    private JWTClaimsSet readClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof claims not readable");
        }
    }

    private void verifyHtm(JWTClaimsSet claims, String httpMethod) {
        Object htm = claims.getClaim("htm");
        if (!(htm instanceof String s) || !s.equalsIgnoreCase(httpMethod)) {
            throw new DPoPProofException("invalid_dpop_proof",
                    "DPoP proof htm '" + htm + "' does not match request method '" + httpMethod + "'");
        }
    }

    private void verifyHtu(JWTClaimsSet claims, String httpUri) {
        Object htu = claims.getClaim("htu");
        if (!(htu instanceof String s)) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof missing htu claim");
        }
        String canonical = canonicalize(s);
        String expected = canonicalize(httpUri);
        if (!canonical.equals(expected)) {
            throw new DPoPProofException("invalid_dpop_proof",
                    "DPoP proof htu '" + canonical + "' does not match request URI '" + expected + "'");
        }
    }

    private Instant verifyIat(JWTClaimsSet claims) {
        java.util.Date issued = claims.getIssueTime();
        if (issued == null) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof missing iat claim");
        }
        Instant now = Instant.now();
        Instant iat = issued.toInstant();
        if (iat.isAfter(now.plus(FRESHNESS))) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof iat in the future");
        }
        if (iat.isBefore(now.minus(FRESHNESS))) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof iat too old");
        }
        return iat;
    }

    private String verifyJti(JWTClaimsSet claims) {
        String jti = claims.getJWTID();
        if (jti == null || jti.isBlank()) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof missing jti claim");
        }
        if (!nonceCache.recordIfAbsent(jti)) {
            throw new DPoPProofException("invalid_dpop_proof", "DPoP proof jti already used");
        }
        return jti;
    }

    private String canonicalize(String uri) {
        int q = uri.indexOf('?');
        if (q >= 0) uri = uri.substring(0, q);
        int h = uri.indexOf('#');
        if (h >= 0) uri = uri.substring(0, h);
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}
