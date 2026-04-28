package dev.pmlsp.openfinance.payments.domain.exception;

public class InvalidConsentStateException extends PaymentsException {
    public InvalidConsentStateException(String message) { super(message); }
}
