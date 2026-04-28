package dev.pmlsp.openfinance.payments.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ofpayments")
public record OfPaymentsProperties(
        Initiator initiator,
        Holder holder,
        Consent consent,
        Payment payment,
        Simulator simulator,
        Mtls mtls
) {

    public record Initiator(String organisationId, String softwareStatementId) {}

    public record Holder(String baseUrl, Duration connectTimeout, Duration readTimeout) {}

    public record Consent(int defaultValidityMinutes, int maxValidityMinutes) {}

    public record Payment(int settleDelayMs) {}

    public record Simulator(double failureRate, int jitterMinMs, int jitterMaxMs) {}

    public record Mtls(boolean enabled, String bundleName) {
        public Mtls {
            if (bundleName == null) {
                bundleName = "";
            }
        }
    }
}
