package dev.pmlsp.openfinance.payments.domain.model;

import dev.pmlsp.openfinance.payments.domain.exception.InvalidPaymentStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentInitiation(
        PaymentId id,
        ConsentId consentId,
        EndToEndId endToEndId,
        Account creditor,
        Account debtor,
        Amount amount,
        PaymentStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant statusUpdatedAt,
        Instant settledAt
) {

    public PaymentInitiation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(consentId, "consentId");
        Objects.requireNonNull(endToEndId, "endToEndId");
        Objects.requireNonNull(creditor, "creditor");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(statusUpdatedAt, "statusUpdatedAt");
    }

    public static PaymentInitiation newReceived(ConsentId consentId, Account creditor,
                                                Account debtor, Amount amount, Ispb debtorIspb,
                                                Instant now) {
        EndToEndId e2e = EndToEndId.generate(debtorIspb, now);
        return new PaymentInitiation(
                new PaymentId(UUID.randomUUID().toString()),
                consentId, e2e, creditor, debtor, amount,
                PaymentStatus.RCVD, null, now, now, null);
    }

    public PaymentInitiation toPending(Instant now) {
        require(status == PaymentStatus.RCVD, "payment cannot move to PDNG from " + status);
        return new PaymentInitiation(id, consentId, endToEndId, creditor, debtor, amount,
                PaymentStatus.PDNG, null, createdAt, now, settledAt);
    }

    public PaymentInitiation accept(Instant now) {
        require(status == PaymentStatus.PDNG || status == PaymentStatus.RCVD,
                "payment cannot move to ACSP from " + status);
        return new PaymentInitiation(id, consentId, endToEndId, creditor, debtor, amount,
                PaymentStatus.ACSP, null, createdAt, now, settledAt);
    }

    public PaymentInitiation settle(Instant now) {
        require(status == PaymentStatus.ACSP, "payment cannot settle from " + status);
        return new PaymentInitiation(id, consentId, endToEndId, creditor, debtor, amount,
                PaymentStatus.ACSC, null, createdAt, now, now);
    }

    public PaymentInitiation reject(String reason, Instant now) {
        require(status != PaymentStatus.ACSC && status != PaymentStatus.RJCT,
                "payment already terminal at " + status);
        return new PaymentInitiation(id, consentId, endToEndId, creditor, debtor, amount,
                PaymentStatus.RJCT, reason, createdAt, now, settledAt);
    }

    public boolean isTerminal() {
        return status == PaymentStatus.ACSC || status == PaymentStatus.RJCT;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidPaymentStateException(message);
        }
    }
}
