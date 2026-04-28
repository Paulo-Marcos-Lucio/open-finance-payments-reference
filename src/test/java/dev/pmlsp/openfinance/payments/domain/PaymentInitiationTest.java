package dev.pmlsp.openfinance.payments.domain;

import dev.pmlsp.openfinance.payments.domain.exception.InvalidPaymentStateException;
import dev.pmlsp.openfinance.payments.domain.model.Account;
import dev.pmlsp.openfinance.payments.domain.model.Account.AccountType;
import dev.pmlsp.openfinance.payments.domain.model.Amount;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.model.Ispb;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;
import dev.pmlsp.openfinance.payments.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentInitiationTest {

    private final Account creditor = new Account(new Ispb("60746948"), "0001",
            "00012345-6", AccountType.CACC);
    private final Account debtor = new Account(new Ispb("99988877"), "0002",
            "1234567", AccountType.CACC);

    @Test
    void receivedToPendingToAcceptedToSettled() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        PaymentInitiation p = PaymentInitiation.newReceived(
                new ConsentId("c1"), creditor, debtor, Amount.brl("12.34"),
                debtor.ispb(), now);
        assertEquals(PaymentStatus.RCVD, p.status());

        PaymentInitiation pending = p.toPending(now.plusSeconds(1));
        assertEquals(PaymentStatus.PDNG, pending.status());

        PaymentInitiation accepted = pending.accept(now.plusSeconds(2));
        assertEquals(PaymentStatus.ACSP, accepted.status());

        PaymentInitiation settled = accepted.settle(now.plusSeconds(3));
        assertEquals(PaymentStatus.ACSC, settled.status());
        assertTrue(settled.isTerminal());
    }

    @Test
    void rejectAtPdng() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        PaymentInitiation p = PaymentInitiation.newReceived(
                        new ConsentId("c1"), creditor, debtor, Amount.brl("12.34"),
                        debtor.ispb(), now)
                .toPending(now);
        PaymentInitiation rejected = p.reject("insufficient funds", now.plusSeconds(1));
        assertEquals(PaymentStatus.RJCT, rejected.status());
        assertTrue(rejected.isTerminal());
        assertEquals("insufficient funds", rejected.rejectionReason());
    }

    @Test
    void cannotSettleWithoutAccept() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        PaymentInitiation p = PaymentInitiation.newReceived(
                new ConsentId("c1"), creditor, debtor, Amount.brl("12.34"),
                debtor.ispb(), now);
        assertThrows(InvalidPaymentStateException.class, () -> p.settle(now.plusSeconds(1)));
    }

    @Test
    void endToEndIdIs32Chars() {
        Instant now = Instant.parse("2026-04-27T10:00:00Z");
        PaymentInitiation p = PaymentInitiation.newReceived(
                new ConsentId("c1"), creditor, debtor, Amount.brl("12.34"),
                debtor.ispb(), now);
        assertEquals(32, p.endToEndId().value().length());
    }
}
