package dev.pmlsp.openfinance.payments.infrastructure.security.dpop;

public class DPoPProofException extends RuntimeException {
    private final String dpopErrorCode;

    public DPoPProofException(String dpopErrorCode, String message) {
        super(message);
        this.dpopErrorCode = dpopErrorCode;
    }

    public String getDpopErrorCode() {
        return dpopErrorCode;
    }
}
