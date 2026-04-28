package dev.pmlsp.openfinance.payments.domain.exception;

public class InvalidPaymentStateException extends PaymentsException {
    public InvalidPaymentStateException(String message) { super(message); }
}
