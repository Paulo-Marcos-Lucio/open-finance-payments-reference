package dev.pmlsp.openfinance.payments.adapter.web;

import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.AccessTokenIntrospector;
import dev.pmlsp.openfinance.payments.infrastructure.security.dpop.DPoPValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration with two profiles:
 * <ul>
 *   <li><strong>default / mock</strong> (any profile that's NOT <code>fapi</code>): permitAll for v0.1.0
 *       behaviour — useful for IT and demo.</li>
 *   <li><strong>fapi</strong>: PISP endpoints under <code>/open-banking/payments/**</code> require
 *       a DPoP-bound access token (RFC 9449). Holder simulator and mock auth endpoints stay open
 *       so the full FAPI flow can be exercised end-to-end without a real authorization server.</li>
 * </ul>
 */
@Configuration
public class WebSecurityConfig {

    @Bean
    @Profile("!fapi")
    @Order(1)
    public SecurityFilterChain mockFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("fapi")
    @Order(1)
    public SecurityFilterChain fapiPispChain(HttpSecurity http,
                                             AccessTokenIntrospector introspector,
                                             DPoPValidator dpopValidator) throws Exception {
        http
                .securityMatcher(new AntPathRequestMatcher("/open-banking/payments/**"))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(new DPoPAuthenticationFilter(introspector, dpopValidator),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Profile("fapi")
    @Order(2)
    public SecurityFilterChain fapiOpenChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
