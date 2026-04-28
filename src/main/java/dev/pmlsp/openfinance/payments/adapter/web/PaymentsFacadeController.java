package dev.pmlsp.openfinance.payments.adapter.web;

import dev.pmlsp.openfinance.payments.adapter.web.dto.WebDtos.AccountRequest;
import dev.pmlsp.openfinance.payments.adapter.web.dto.WebDtos.ConsentResponse;
import dev.pmlsp.openfinance.payments.adapter.web.dto.WebDtos.CreateConsentRequest;
import dev.pmlsp.openfinance.payments.adapter.web.dto.WebDtos.CreatePaymentRequest;
import dev.pmlsp.openfinance.payments.adapter.web.dto.WebDtos.PaymentResponse;
import dev.pmlsp.openfinance.payments.adapter.web.dto.WebDtos.SubjectRequest;
import dev.pmlsp.openfinance.payments.domain.model.Account;
import dev.pmlsp.openfinance.payments.domain.model.Account.AccountType;
import dev.pmlsp.openfinance.payments.domain.model.Amount;
import dev.pmlsp.openfinance.payments.domain.model.Consent;
import dev.pmlsp.openfinance.payments.domain.model.ConsentId;
import dev.pmlsp.openfinance.payments.domain.model.Document;
import dev.pmlsp.openfinance.payments.domain.model.Document.DocumentType;
import dev.pmlsp.openfinance.payments.domain.model.Ispb;
import dev.pmlsp.openfinance.payments.domain.model.PaymentId;
import dev.pmlsp.openfinance.payments.domain.model.PaymentInitiation;
import dev.pmlsp.openfinance.payments.domain.model.Subject;
import dev.pmlsp.openfinance.payments.domain.port.in.CreateConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.CreateConsentUseCase.CreateConsentCommand;
import dev.pmlsp.openfinance.payments.domain.port.in.CreatePaymentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.CreatePaymentUseCase.CreatePaymentCommand;
import dev.pmlsp.openfinance.payments.domain.port.in.GetConsentUseCase;
import dev.pmlsp.openfinance.payments.domain.port.in.GetPaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/open-banking/payments/v1")
public class PaymentsFacadeController {

    private final CreateConsentUseCase createConsent;
    private final GetConsentUseCase getConsent;
    private final CreatePaymentUseCase createPayment;
    private final GetPaymentUseCase getPayment;

    public PaymentsFacadeController(CreateConsentUseCase createConsent,
                                    GetConsentUseCase getConsent,
                                    CreatePaymentUseCase createPayment,
                                    GetPaymentUseCase getPayment) {
        this.createConsent = createConsent;
        this.getConsent = getConsent;
        this.createPayment = createPayment;
        this.getPayment = getPayment;
    }

    @PostMapping("/consents")
    public ResponseEntity<ConsentResponse> createConsent(@RequestBody @Valid CreateConsentRequest body) {
        Subject loggedUser = toSubject(body.loggedUser(), Subject.SubjectType.LOGGED_USER);
        Subject businessEntity = body.businessEntity() == null
                ? null : toSubject(body.businessEntity(), Subject.SubjectType.BUSINESS_ENTITY);
        Account creditor = toAccount(body.creditor());
        Amount amount = new Amount(new BigDecimal(body.amount()).setScale(2, RoundingMode.UNNECESSARY),
                body.currency());

        Consent consent = createConsent.create(new CreateConsentCommand(
                loggedUser, businessEntity, creditor, amount, body.validityMinutes()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(consent));
    }

    @GetMapping("/consents/{consentId}")
    public ConsentResponse getConsent(@PathVariable String consentId) {
        Consent consent = getConsent.get(new ConsentId(consentId));
        return toResponse(consent);
    }

    @PostMapping("/pix/payments")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody @Valid CreatePaymentRequest body) {
        PaymentInitiation payment = createPayment.create(new CreatePaymentCommand(
                new ConsentId(body.consentId()),
                toAccount(body.debtor())));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(payment));
    }

    @GetMapping("/pix/payments/{paymentId}")
    public PaymentResponse getPayment(@PathVariable String paymentId) {
        PaymentInitiation payment = getPayment.get(new PaymentId(paymentId));
        return toResponse(payment);
    }

    private static Subject toSubject(SubjectRequest req, Subject.SubjectType type) {
        Document doc = new Document(DocumentType.valueOf(req.documentType()), req.document());
        return new Subject(type, doc, req.name());
    }

    private static Account toAccount(AccountRequest req) {
        return new Account(new Ispb(req.ispb()), req.issuer(), req.number(),
                AccountType.valueOf(req.type()));
    }

    private static ConsentResponse toResponse(Consent c) {
        return new ConsentResponse(c.id().value(), c.status().name(),
                c.creditor().ispb().value(),
                c.amount().asString(), c.amount().currency(),
                c.createdAt(), c.expiresAt(), c.statusUpdatedAt());
    }

    private static PaymentResponse toResponse(PaymentInitiation p) {
        return new PaymentResponse(
                p.id().value(),
                p.consentId().value(),
                p.endToEndId().value(),
                p.status().name(),
                p.rejectionReason(),
                p.creditor().ispb().value(),
                p.debtor() == null ? null : p.debtor().ispb().value(),
                p.amount().asString(),
                p.createdAt(),
                p.statusUpdatedAt(),
                p.settledAt());
    }
}
