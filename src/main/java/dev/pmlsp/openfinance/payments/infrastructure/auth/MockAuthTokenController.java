package dev.pmlsp.openfinance.payments.infrastructure.auth;

import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.DPoPProof;
import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.DPoPProofException;
import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.DPoPValidator;
import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.MockAuthKeystore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * Mock authorization server endpoint that issues sender-constrained access tokens
 * bound to the caller's DPoP key (cnf.jkt = thumbprint of caller JWK).
 * <p>
 * Single grant supported in v0.2.0: {@code client_credentials}-style with no client secret.
 * Real BCB profile requires private_key_jwt assertion (RFC 7523) — roadmap for v0.3.0.
 * <p>
 * Endpoint is part of the same Spring context (mirrors the holder simulator pattern from v0.1.0).
 */
@RestController
@RequestMapping("/mock-auth")
public class MockAuthTokenController {

    private static final String DEFAULT_SCOPE = "payments";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private final DPoPValidator dpopValidator;
    private final MockAuthKeystore keystore;

    public MockAuthTokenController(DPoPValidator dpopValidator, MockAuthKeystore keystore) {
        this.dpopValidator = dpopValidator;
        this.keystore = keystore;
    }

    @PostMapping("/token")
    public ResponseEntity<?> issueToken(
            @RequestHeader(name = "DPoP", required = false) String dpopProof,
            @RequestHeader(name = "X-Client-Id", required = false, defaultValue = "demo-client") String clientId,
            @RequestHeader(name = "X-Subject", required = false, defaultValue = "demo-subject") String subject,
            HttpServletRequest request) {

        if (dpopProof == null || dpopProof.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "invalid_request",
                    "error_description", "DPoP header required to bind token to caller key"));
        }

        try {
            DPoPProof proof = dpopValidator.validate(
                    dpopProof,
                    request.getMethod(),
                    request.getRequestURL().toString(),
                    null);

            String token = keystore.issueAccessToken(clientId, subject, DEFAULT_SCOPE,
                    proof.thumbprint(), TOKEN_TTL);

            return ResponseEntity.ok(Map.of(
                    "access_token", token,
                    "token_type", "DPoP",
                    "expires_in", TOKEN_TTL.toSeconds(),
                    "scope", DEFAULT_SCOPE));
        } catch (DPoPProofException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", e.getDpopErrorCode(),
                    "error_description", e.getMessage()));
        }
    }
}
