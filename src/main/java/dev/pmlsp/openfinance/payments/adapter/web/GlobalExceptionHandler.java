package dev.pmlsp.openfinance.payments.adapter.web;

import dev.pmlsp.openfinance.payments.adapter.web.dto.WebDtos.ErrorResponse;
import dev.pmlsp.openfinance.payments.domain.exception.ConsentNotAuthorisedException;
import dev.pmlsp.openfinance.payments.domain.exception.ConsentNotFoundException;
import dev.pmlsp.openfinance.payments.domain.exception.HolderUnavailableException;
import dev.pmlsp.openfinance.payments.domain.exception.InvalidConsentStateException;
import dev.pmlsp.openfinance.payments.domain.exception.InvalidPaymentStateException;
import dev.pmlsp.openfinance.payments.domain.exception.PaymentNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ConsentNotFoundException.class, PaymentNotFoundException.class})
    public ResponseEntity<ErrorResponse> notFound(RuntimeException e) {
        return error(HttpStatus.NOT_FOUND, "not_found", e.getMessage());
    }

    @ExceptionHandler({InvalidConsentStateException.class, InvalidPaymentStateException.class,
            ConsentNotAuthorisedException.class, IllegalArgumentException.class,
            ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        String first = e.getBindingResult().getAllErrors().stream()
                .findFirst().map(err -> err.getDefaultMessage()).orElse("validation failed");
        return error(HttpStatus.BAD_REQUEST, "invalid_request", first);
    }

    @ExceptionHandler(HolderUnavailableException.class)
    public ResponseEntity<ErrorResponse> holderDown(HolderUnavailableException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "holder_unavailable", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", e.getClass().getSimpleName());
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, Instant.now()));
    }
}
