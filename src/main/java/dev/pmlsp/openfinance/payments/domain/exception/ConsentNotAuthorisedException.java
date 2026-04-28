package dev.pmlsp.openfinance.payments.domain.exception;

public class ConsentNotAuthorisedException extends PaymentsException {
    public ConsentNotAuthorisedException(String consentId, String currentStatus) {
        super("consent " + consentId + " is not AUTHORISED (current=" + currentStatus + ")");
    }
}
