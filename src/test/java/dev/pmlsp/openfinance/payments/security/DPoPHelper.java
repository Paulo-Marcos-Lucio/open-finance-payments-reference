package dev.pmlsp.openfinance.payments.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.UUID;

/**
 * Test helper for generating DPoP proof JWTs from a fresh ephemeral key.
 * Mirrors what a real PISP client SDK would do per RFC 9449.
 */
public class DPoPHelper {

    private final ECKey key;

    public DPoPHelper() {
        try {
            this.key = new ECKeyGenerator(Curve.P_256)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String thumbprint() {
        try {
            return key.toPublicJWK().computeThumbprint().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String sign(String htm, String htu) {
        return sign(htm, htu, Instant.now(), UUID.randomUUID().toString());
    }

    public String sign(String htm, String htu, Instant iat, String jti) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issueTime(java.util.Date.from(iat))
                    .jwtID(jti)
                    .claim("htm", htm)
                    .claim("htu", htu)
                    .build();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(key.toPublicJWK())
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            JWSSigner signer = new ECDSASigner(key);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
