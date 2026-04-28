package dev.pmlsp.openfinance.payments.domain.exception;

public abstract class PaymentsException extends RuntimeException {
    protected PaymentsException(String message) { super(message); }
    protected PaymentsException(String message, Throwable cause) { super(message, cause); }
}
