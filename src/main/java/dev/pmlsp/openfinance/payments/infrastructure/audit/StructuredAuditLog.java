package dev.pmlsp.openfinance.payments.infrastructure.audit;

import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditLog;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StructuredAuditLog implements AuditLog {

    private static final Logger LOG = LoggerFactory.getLogger("audit");
    private final MeterRegistry registry;

    public StructuredAuditLog(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void emit(AuditEvent event) {
        LOG.info("audit kind={} entity={} outcome={} details={}",
                event.kind(), event.entityId(), event.outcome(), event.details());
        registry.counter("ofpayments.audit.events",
                "kind", event.kind().name(),
                "outcome", event.outcome() == null ? "n/a" : event.outcome())
                .increment();
    }
}
