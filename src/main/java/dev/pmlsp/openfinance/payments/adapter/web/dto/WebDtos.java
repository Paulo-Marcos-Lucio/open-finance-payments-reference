package dev.pmlsp.openfinance.payments.adapter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public final class WebDtos {

    private WebDtos() {}

    public record SubjectRequest(
            @NotBlank String name,
            @NotBlank String document,
            @NotBlank @Pattern(regexp = "CPF|CNPJ") String documentType
    ) {}

    public record AccountRequest(
            @NotBlank @Pattern(regexp = "\\d{8}") String ispb,
            String issuer,
            @NotBlank String number,
            @NotBlank @Pattern(regexp = "CACC|SVGS|SLRY|TRAN") String type
    ) {}

    public record CreateConsentRequest(
            @NotNull @Valid SubjectRequest loggedUser,
            @Valid SubjectRequest businessEntity,
            @NotNull @Valid AccountRequest creditor,
            @NotBlank @Pattern(regexp = "\\d+(\\.\\d{1,2})?") String amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @Positive Integer validityMinutes
    ) {}

    public record ConsentResponse(
            String consentId,
            String status,
            String creditorIspb,
            String amount,
            String currency,
            Instant createdAt,
            Instant expiresAt,
            Instant statusUpdatedAt
    ) {}

    public record CreatePaymentRequest(
            @NotBlank String consentId,
            @NotNull @Valid AccountRequest debtor
    ) {}

    public record PaymentResponse(
            String paymentId,
            String consentId,
            String endToEndId,
            String status,
            String rejectionReason,
            String creditorIspb,
            String debtorIspb,
            String amount,
            Instant createdAt,
            Instant statusUpdatedAt,
            Instant settledAt
    ) {}

    public record ErrorResponse(String code, String message, Instant occurredAt) {}
}
