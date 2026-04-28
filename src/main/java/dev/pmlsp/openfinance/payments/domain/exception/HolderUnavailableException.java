package dev.pmlsp.openfinance.payments.domain.exception;

public class HolderUnavailableException extends PaymentsException {
    public HolderUnavailableException(String message) { super(message); }
    public HolderUnavailableException(String message, Throwable cause) { super(message, cause); }
}
