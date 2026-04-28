package dev.pmlsp.openfinance.payments.adapter.web;

import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.AccessTokenIntrospector;
import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.AccessTokenIntrospector.IntrospectedToken;
import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.DPoPProofException;
import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.DPoPValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates DPoP-bound access tokens (RFC 9449) on each request to PISP endpoints.
 * <p>
 * Wire protocol: clients send <code>Authorization: DPoP &lt;access_token&gt;</code> +
 * <code>DPoP: &lt;proof_jwt&gt;</code>. The proof is signed by the caller's private key,
 * the access token's <code>cnf.jkt</code> claim binds it to that same key's thumbprint —
 * so token replay from a different machine fails the cnf check.
 * <p>
 * Failed validation produces 401 Unauthorized with <code>WWW-Authenticate: DPoP error="..."</code>.
 */
public class DPoPAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_SCHEME = "DPoP ";

    private final AccessTokenIntrospector introspector;
    private final DPoPValidator dpopValidator;

    public DPoPAuthenticationFilter(AccessTokenIntrospector introspector, DPoPValidator dpopValidator) {
        this.introspector = introspector;
        this.dpopValidator = dpopValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authz = request.getHeader("Authorization");
        if (authz == null || !authz.startsWith(AUTH_SCHEME)) {
            send401(response, "invalid_token", "Authorization header must use DPoP scheme");
            return;
        }
        String accessToken = authz.substring(AUTH_SCHEME.length()).trim();
        if (accessToken.isEmpty()) {
            send401(response, "invalid_token", "access token is empty");
            return;
        }

        String dpopProof = request.getHeader("DPoP");
        if (dpopProof == null || dpopProof.isBlank()) {
            send401(response, "invalid_token", "DPoP header required");
            return;
        }

        try {
            IntrospectedToken token = introspector.introspect(accessToken);
            String requestUri = request.getRequestURL().toString();
            dpopValidator.validate(dpopProof, request.getMethod(), requestUri, token.cnfJkt());

            DPoPAuthentication auth = new DPoPAuthentication(token);
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (DPoPProofException e) {
            SecurityContextHolder.clearContext();
            send401(response, e.getDpopErrorCode(), e.getMessage());
        }
    }

    private void send401(HttpServletResponse response, String errorCode, String description) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate",
                "DPoP error=\"" + errorCode + "\", error_description=\"" + description + "\"");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + errorCode + "\",\"error_description\":\""
                + description.replace("\"", "\\\"") + "\"}");
    }

    public static final class DPoPAuthentication extends AbstractAuthenticationToken {
        private final IntrospectedToken token;

        public DPoPAuthentication(IntrospectedToken token) {
            super(List.of(new SimpleGrantedAuthority("SCOPE_" + token.scope())));
            this.token = token;
        }

        @Override
        public Object getCredentials() {
            return token;
        }

        @Override
        public Object getPrincipal() {
            return token.subject();
        }

        public IntrospectedToken token() {
            return token;
        }
    }
}
