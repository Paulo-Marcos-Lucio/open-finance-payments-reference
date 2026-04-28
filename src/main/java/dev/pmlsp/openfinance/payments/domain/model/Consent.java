package dev.pmlsp.openfinance.payments.domain.model;

import dev.pmlsp.openfinance.payments.domain.exception.InvalidConsentStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Consent(
        ConsentId id,
        Subject loggedUser,
        Subject businessEntity,
        Account creditor,
        Amount amount,
        ConsentStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant statusUpdatedAt
) {

    public Consent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(loggedUser, "loggedUser");
        Objects.requireNonNull(creditor, "creditor");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(statusUpdatedAt, "statusUpdatedAt");
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be >= createdAt");
        }
    }

    public static Consent newReceived(Subject loggedUser, Subject businessEntity,
                                      Account creditor, Amount amount,
                                      Instant now, Instant expiresAt) {
        return new Consent(
                new ConsentId("urn:bancoex:C1DD33123-" + UUID.randomUUID()),
                loggedUser, businessEntity, creditor, amount,
                ConsentStatus.RCVD,
                now, expiresAt, now);
    }

    public Consent authorise(Instant now) {
        require(status == ConsentStatus.RCVD,
                "consent " + id.value() + " cannot be authorised from " + status);
        if (now.isAfter(expiresAt)) {
            throw new InvalidConsentStateException("consent " + id.value() + " expired at " + expiresAt);
        }
        return withStatus(ConsentStatus.AUTHORISED, now);
    }

    public Consent reject(Instant now) {
        require(status == ConsentStatus.RCVD || status == ConsentStatus.AUTHORISED,
                "consent " + id.value() + " cannot be rejected from " + status);
        return withStatus(ConsentStatus.REJECTED, now);
    }

    public Consent consume(Instant now) {
        require(status == ConsentStatus.AUTHORISED,
                "consent " + id.value() + " cannot be consumed from " + status);
        return withStatus(ConsentStatus.CONSUMED, now);
    }

    public Consent revoke(Instant now) {
        require(status == ConsentStatus.AUTHORISED || status == ConsentStatus.RCVD,
                "consent " + id.value() + " cannot be revoked from " + status);
        return withStatus(ConsentStatus.REVOKED, now);
    }

    public Consent expireIfNeeded(Instant now) {
        if (status == ConsentStatus.RCVD && now.isAfter(expiresAt)) {
            return withStatus(ConsentStatus.EXPIRED, now);
        }
        return this;
    }

    public boolean isAuthorised() {
        return status == ConsentStatus.AUTHORISED;
    }

    private Consent withStatus(ConsentStatus next, Instant now) {
        return new Consent(id, loggedUser, businessEntity, creditor, amount,
                next, createdAt, expiresAt, now);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidConsentStateException(message);
        }
    }
}
