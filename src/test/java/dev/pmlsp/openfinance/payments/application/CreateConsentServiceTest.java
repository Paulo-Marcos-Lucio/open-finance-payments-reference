package dev.pmlsp.openfinance.payments.application;

import dev.pmlsp.openfinance.payments.application.consent.CreateConsentService;
import dev.pmlsp.openfinance.payments.domain.model.Account;
import dev.pmlsp.openfinance.payments.domain.model.Account.AccountType;
import dev.pmlsp.openfinance.payments.domain.model.Amount;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.model.ConsentStatus;
import dev.pmlsp.openfinance.payments.domain.model.Document;
import dev.pmlsp.openfinance.payments.domain.model.Document.DocumentType;
import dev.pmlsp.openfinance.payments.domain.model.Ispb;
import dev.pmlsp.openfinance.payments.domain.model.Subject;
import dev.pmlsp.openfinance.payments.domain.port.in.CreateConsentUseCase.CreateConsentCommand;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditEvent;
import dev.pmlsp.openfinance.payments.domain.port.out.AuditLog;
import dev.pmlsp.openfinance.payments.domain.port.out.Clock;
import dev.pmlsp.openfinance.payments.domain.port.out.ConsentRepository;
import dev.pmlsp.openfinance.payments.domain.port.out.HolderConsentGateway;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateConsentServiceTest {

    @Test
    void createsConsentInRcvdAndCallsHolderAndAudits() {
        var consents = new ConcurrentHashMap<String, Consent>();
        var holderRegistered = new ArrayList<Consent>();
        var audited = new ArrayList<AuditEvent>();

        Instant now = Instant.parse("2026-04-27T12:00:00Z");

        Clock clock = () -> now;
        ConsentRepository repo = new ConsentRepository() {
            public void save(Consent c) { consents.put(c.id().value(), c); }
            public Optional<Consent> findById(ConsentId id) {
                return Optional.ofNullable(consents.get(id.value()));
            }
        };
        HolderConsentGateway holder = new HolderConsentGateway() {
            public void registerConsent(Consent c) { holderRegistered.add(c); }
            public String authorisationUrl(Consent c) { return "/auth/" + c.id().value(); }
        };
        AuditLog audit = audited::add;

        var service = new CreateConsentService(repo, holder, audit, clock,
                Duration.ofMinutes(5), Duration.ofMinutes(60));

        Subject loggedUser = new Subject(Subject.SubjectType.LOGGED_USER,
                new Document(DocumentType.CPF, "12345678901"), "Fulano");
        Account creditor = new Account(new Ispb("60746948"), "0001",
                "00012345-6", AccountType.CACC);

        Consent created = service.create(new CreateConsentCommand(
                loggedUser, null, creditor, Amount.brl("99.00"), null));

        assertEquals(ConsentStatus.RCVD, created.status());
        assertEquals(1, consents.size());
        assertEquals(1, holderRegistered.size());
        assertEquals(1, audited.size());
        assertEquals(AuditEvent.AuditKind.CONSENT_CREATED, audited.get(0).kind());
        assertEquals(now.plus(Duration.ofMinutes(5)), created.expiresAt());
    }

    @Test
    void clampsValidityToMaximum() {
        var consents = new ConcurrentHashMap<String, Consent>();
        Clock clock = () -> Instant.parse("2026-04-27T12:00:00Z");
        ConsentRepository repo = new ConsentRepository() {
            public void save(Consent c) { consents.put(c.id().value(), c); }
            public Optional<Consent> findById(ConsentId id) {
                return Optional.ofNullable(consents.get(id.value()));
            }
        };
        HolderConsentGateway holder = new HolderConsentGateway() {
            public void registerConsent(Consent c) {}
            public String authorisationUrl(Consent c) { return ""; }
        };
        List<AuditEvent> audited = new ArrayList<>();

        var service = new CreateConsentService(repo, holder, audited::add, clock,
                Duration.ofMinutes(5), Duration.ofMinutes(60));

        Subject u = new Subject(Subject.SubjectType.LOGGED_USER,
                new Document(DocumentType.CPF, "12345678901"), "Fulano");
        Account creditor = new Account(new Ispb("60746948"), "0001",
                "00012345-6", AccountType.CACC);

        Consent created = service.create(new CreateConsentCommand(
                u, null, creditor, Amount.brl("99.00"), 999));

        Duration validity = Duration.between(created.createdAt(), created.expiresAt());
        assertTrue(validity.compareTo(Duration.ofMinutes(60)) == 0,
                "expected clamp to 60min, got " + validity);
    }
}
