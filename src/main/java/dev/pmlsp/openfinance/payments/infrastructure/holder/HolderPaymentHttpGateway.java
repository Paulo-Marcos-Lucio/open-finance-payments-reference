package dev.pmlsp.openfinance.payments.infrastructure.holder;

import dev.pmlsp.openfinance.payments.domain.exception.HolderUnavailableException;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderPaymentGateway;
import dev.pmlsp.openfinance.payments.infrastructure.config.OfPaymentsProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Objects;

@Component
public class HolderPaymentHttpGateway implements HolderPaymentGateway {

    private final RestClient client;

    public HolderPaymentHttpGateway(OfPaymentsProperties props, RestClient.Builder builder) {
        this.client = builder.baseUrl(props.holder().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    @Retry(name = "holder-payment")
    @CircuitBreaker(name = "holder-payment")
    public PaymentSettlementResult submit(PaymentInitiation payment) {
        try {
            HolderPaymentResponse response = client.post()
                    .uri("/holder/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentId", payment.id().value(),
                            "consentId", payment.consentId().value(),
                            "endToEndId", payment.endToEndId().value(),
                            "creditorIspb", payment.creditor().ispb().value(),
                            "debtorIspb", payment.debtor().ispb().value(),
                            "amount", payment.amount().asString(),
                            "currency", payment.amount().currency()))
                    .retrieve()
                    .body(HolderPaymentResponse.class);
            Objects.requireNonNull(response, "holder response was null");
            return new PaymentSettlementResult(response.accepted(), response.rejectionReason());
        } catch (RestClientException e) {
            throw new HolderUnavailableException(
                    "submit payment " + payment.id().value() + " failed: " + e.getMessage(), e);
        }
    }

    private record HolderPaymentResponse(boolean accepted, String rejectionReason) {}
}
