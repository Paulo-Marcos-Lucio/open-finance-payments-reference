package dev.pmlsp.openfinance.payments.infrastructure.simulator;

import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentId;
import dev.pmlsp.openfinance.payments.domain.port.in.AuthoriseConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.SettlePaymentUseCase;
import dev.pmlsp.openfinance.payments.infrastructure.config.OfPaymentsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/sim")
public class HolderSimulatorController {

    private static final Logger LOG = LoggerFactory.getLogger(HolderSimulatorController.class);

    private final AuthoriseConsentUseCase authoriseConsent;
    private final SettlePaymentUseCase settlePayment;
    private final OfPaymentsProperties props;
    private final ScheduledExecutorService scheduler;
    private final MeterRegistry registry;
    private final Set<String> registeredConsents = ConcurrentHashMap.newKeySet();

    public HolderSimulatorController(AuthoriseConsentUseCase authoriseConsent,
                                     SettlePaymentUseCase settlePayment,
                                     OfPaymentsProperties props,
                                     ScheduledExecutorService scheduler,
                                     MeterRegistry registry) {
        this.authoriseConsent = authoriseConsent;
        this.settlePayment = settlePayment;
        this.props = props;
        this.scheduler = scheduler;
        this.registry = registry;
    }

    @PostMapping("/holder/consents")
    public Map<String, Object> registerConsent(@RequestBody Map<String, Object> body) {
        injectFault();
        applyJitter();
        String consentId = String.valueOf(body.get("consentId"));
        registeredConsents.add(consentId);
        registry.counter("ofpayments.simulator.consents.registered").increment();
        LOG.info("simulator: holder registered consent {}", consentId);
        return Map.of("consentId", consentId, "status", "ACCEPTED");
    }

    @PostMapping("/holder/consents/{consentId}/authorise")
    public Map<String, Object> authoriseConsent(@PathVariable String consentId) {
        if (!registeredConsents.contains(consentId)) {
            return Map.of("authorised", false, "reason", "consent not registered");
        }
        authoriseConsent.authorise(new ConsentId(consentId));
        registry.counter("ofpayments.simulator.consents.authorised").increment();
        return Map.of("authorised", true);
    }

    @PostMapping("/holder/payments")
    public Map<String, Object> submitPayment(@RequestBody Map<String, Object> body) {
        injectFault();
        applyJitter();
        String paymentId = String.valueOf(body.get("paymentId"));
        registry.counter("ofpayments.simulator.payments.received").increment();
        LOG.info("simulator: holder received payment {}", paymentId);
        scheduler.schedule(() -> {
            try {
                settlePayment.settle(new PaymentId(paymentId));
                registry.counter("ofpayments.simulator.payments.settled").increment();
            } catch (Exception e) {
                LOG.warn("simulator: settlement of {} failed: {}", paymentId, e.getMessage());
            }
        }, props.payment().settleDelayMs(), java.util.concurrent.TimeUnit.MILLISECONDS);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("accepted", true);
        response.put("rejectionReason", null);
        return response;
    }

    private void injectFault() {
        if (props.simulator().failureRate() > 0
                && ThreadLocalRandom.current().nextDouble() < props.simulator().failureRate()) {
            registry.counter("ofpayments.simulator.failures.injected").increment();
            throw new SimulatedHolderFailure("simulator injected fault");
        }
    }

    private void applyJitter() {
        int min = props.simulator().jitterMinMs();
        int max = props.simulator().jitterMaxMs();
        if (max <= 0 || max <= min) {
            return;
        }
        int waitMs = ThreadLocalRandom.current().nextInt(min, max + 1);
        try {
            Thread.sleep(waitMs);
            registry.counter("ofpayments.simulator.jitter.applied").increment();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class SimulatedHolderFailure extends RuntimeException {
        SimulatedHolderFailure(String message) { super(message); }
    }
}
