package dev.pmlsp.openfinance.payments.domain.exception;

public class ConsentNotFoundException extends PaymentsException {
    public ConsentNotFoundException(String consentId) {
        super("consent not found: " + consentId);
    }
}
