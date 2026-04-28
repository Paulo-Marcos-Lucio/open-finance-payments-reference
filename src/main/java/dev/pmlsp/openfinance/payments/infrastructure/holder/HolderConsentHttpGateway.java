package dev.pmlsp.openfinance.payments.infrastructure.holder;

import dev.pmlsp.openfinance.payments.domain.exception.HolderUnavailableException;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderConsentGateway;
import dev.pmlsp.openfinance.payments.infrastructure.config.OfPaymentsProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class HolderConsentHttpGateway implements HolderConsentGateway {

    private final RestClient client;
    private final String baseUrl;

    public HolderConsentHttpGateway(OfPaymentsProperties props, RestClient.Builder builder) {
        this.baseUrl = props.holder().baseUrl();
        this.client = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    @Retry(name = "holder-consent")
    @CircuitBreaker(name = "holder-consent")
    @RateLimiter(name = "holder-consent")
    public void registerConsent(Consent consent) {
        try {
            client.post()
                    .uri("/holder/consents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "consentId", consent.id().value(),
                            "creditorIspb", consent.creditor().ispb().value(),
                            "amount", consent.amount().asString(),
                            "currency", consent.amount().currency()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new HolderUnavailableException(
                    "register consent " + consent.id().value() + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String authorisationUrl(Consent consent) {
        return baseUrl + "/holder/auth?consent=" + consent.id().value();
    }
}
