package dev.pmlsp.openfinance.payments.infrastructure.config;

import dev.pmlsp.openfinance.payments.application.consent.AuthoriseConsentService;
import dev.pmlsp.openfinance.payments.application.consent.CreateConsentService;
import dev.pmlsp.openfinance.payments.application.consent.GetConsentService;
import dev.pmlsp.openfinance.payments.application.payment.CreatePaymentService;
import dev.pmlsp.openfinance.payments.application.payment.GetPaymentService;
import dev.pmlsp.openfinance.payments.application.payment.SettlePaymentService;
import dev.pmlsp.openfinance.payments.domain.port.in.AuthoriseConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.CreateConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.CreatePaymentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.GetConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.GetPaymentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.SettlePaymentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditLog;
import dev.pmlsp.openfinance.payments.domain.port.out.Clock;
import dev.pmlsp.openfinance.payments.domain.port.out.ConsentRepository;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderConsentGateway;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderPaymentGateway;
import dev.pmlsp.openfinance.payments.domain.port.out.PaymentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OfPaymentsAutoConfig {

    @Bean
    public Clock systemClock() {
        return new SystemClock();
    }

    @Bean
    public CreateConsentUseCase createConsentUseCase(ConsentRepository consents,
                                                     HolderConsentGateway holder,
                                                     AuditLog audit, Clock clock,
                                                     OfPaymentsProperties props) {
        return new CreateConsentService(consents, holder, audit, clock,
                Duration.ofMinutes(props.consent().defaultValidityMinutes()),
                Duration.ofMinutes(props.consent().maxValidityMinutes()));
    }

    @Bean
    public GetConsentUseCase getConsentUseCase(ConsentRepository consents, Clock clock) {
        return new GetConsentService(consents, clock);
    }

    @Bean
    public AuthoriseConsentUseCase authoriseConsentUseCase(ConsentRepository consents,
                                                           AuditLog audit, Clock clock) {
        return new AuthoriseConsentService(consents, audit, clock);
    }

    @Bean
    public CreatePaymentUseCase createPaymentUseCase(ConsentRepository consents,
                                                     PaymentRepository payments,
                                                     HolderPaymentGateway holder,
                                                     AuditLog audit, Clock clock) {
        return new CreatePaymentService(consents, payments, holder, audit, clock);
    }

    @Bean
    public GetPaymentUseCase getPaymentUseCase(PaymentRepository payments) {
        return new GetPaymentService(payments);
    }

    @Bean
    public SettlePaymentUseCase settlePaymentUseCase(PaymentRepository payments,
                                                     AuditLog audit, Clock clock) {
        return new SettlePaymentService(payments, audit, clock);
    }
}
