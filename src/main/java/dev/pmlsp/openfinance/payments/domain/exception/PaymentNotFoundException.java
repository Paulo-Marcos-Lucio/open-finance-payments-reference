package dev.pmlsp.openfinance.payments.domain.exception;

public class PaymentNotFoundException extends PaymentsException {
    public PaymentNotFoundException(String paymentId) {
        super("payment not found: " + paymentId);
    }
}
