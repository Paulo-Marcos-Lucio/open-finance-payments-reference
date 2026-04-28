package dev.pmlsp.openfinance.payments.domain;

import dev.pmlsp.openfinance.payments.domain.exception.InvalidConsentStateException;
import dev.pmlsp.openfinance.payments.domain.model.Account;
import dev.pmlsp.openfinance.payments.domain.model.Account.AccountType;
import dev.pmlsp.openfinance.payments.domain.model.Amount;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentStatus;
import dev.pmlsp.openfinance.payments.domain.model.Document;
import dev.pmlsp.openfinance.payments.domain.model.Document.DocumentType;
import dev.pmlsp.openfinance.payments.domain.model.Ispb;
import dev.pmlsp.openfinance.payments.domain.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsentTest {

    private final Subject loggedUser = new Subject(
            Subject.SubjectType.LOGGED_USER,
            new Document(DocumentType.CPF, "12345678901"), "Fulano");
    private final Account creditor = new Account(new Ispb("60746948"), "0001",
            "00012345-6", AccountType.CACC);
    private final Amount amount = Amount.brl("100.00");

    @Test
    void newConsentStartsAtRcvd() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        Consent c = Consent.newReceived(loggedUser, null, creditor, amount, now, now.plusSeconds(300));
        assertEquals(ConsentStatus.RCVD, c.status());
    }

    @Test
    void authoriseFromRcvd() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        Consent c = Consent.newReceived(loggedUser, null, creditor, amount, now, now.plusSeconds(300));
        Consent authorised = c.authorise(now.plusSeconds(10));
        assertEquals(ConsentStatus.AUTHORISED, authorised.status());
    }

    @Test
    void cannotAuthoriseTwice() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        Consent c = Consent.newReceived(loggedUser, null, creditor, amount, now, now.plusSeconds(300))
                .authorise(now);
        assertThrows(InvalidConsentStateException.class, () -> c.authorise(now.plusSeconds(1)));
    }

    @Test
    void cannotAuthoriseExpired() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        Consent c = Consent.newReceived(loggedUser, null, creditor, amount, now, now.plusSeconds(60));
        assertThrows(InvalidConsentStateException.class,
                () -> c.authorise(now.plusSeconds(120)));
    }

    @Test
    void expireOnlyWhenStillRcvd() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        Consent c = Consent.newReceived(loggedUser, null, creditor, amount, now, now.plusSeconds(60));
        Consent expired = c.expireIfNeeded(now.plusSeconds(120));
        assertEquals(ConsentStatus.EXPIRED, expired.status());

        Consent authorised = c.authorise(now);
        Consent stillAuthorised = authorised.expireIfNeeded(now.plusSeconds(999));
        assertEquals(ConsentStatus.AUTHORISED, stillAuthorised.status());
    }

    @Test
    void consumeRequiresAuthorised() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        Consent c = Consent.newReceived(loggedUser, null, creditor, amount, now, now.plusSeconds(300));
        assertThrows(InvalidConsentStateException.class, () -> c.consume(now));
    }
}
