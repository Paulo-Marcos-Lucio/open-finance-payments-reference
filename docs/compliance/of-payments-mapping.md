# Mapeamento Open Finance Brasil — Spec → Código

> Mapping de cada feature do reference pro item correspondente da spec do Open Finance
> Brasil (Fase 4 — Iniciação de Pagamento). Útil como compliance gap analysis.

## Consent flow

| Spec | Onde no código |
|---|---|
| `POST /consents` | `PaymentsFacadeController.createConsent()` |
| Consent body com `loggedUser` (subject obrigatório) e `businessEntity` (opcional, PJ) | `WebDtos.CreateConsentRequest` |
| `data.creditor` (account com ISPB+number+type) | `WebDtos.AccountRequest` → `domain/model/Account.java` |
| `data.payment.amount` em string com 2 decimais + `currency` (ISO 4217) | `domain/model/Amount.java` (validação) |
| `data.expirationDateTime` (default 5min, max 1d na spec atual) | `OfPaymentsProperties.Consent` (`default-validity-minutes`, `max-validity-minutes`) |
| Status: `AWAITING_AUTHORISATION` (= `RCVD`) → `AUTHORISED` → `CONSUMED|REJECTED|REVOKED` | `domain/model/Consent.java` + state machine |
| `GET /consents/{id}` retorna status + statusUpdateDateTime | `PaymentsFacadeController.getConsent()` |
| Lazy expiration | `GetConsentService.get()` aplica `consent.expireIfNeeded(now)` |

## Payment flow

| Spec | Onde no código |
|---|---|
| `POST /pix/payments` (consent-driven) | `PaymentsFacadeController.createPayment()` |
| Verificação que consent está AUTHORISED | `CreatePaymentService.create()` → `ConsentNotAuthorisedException` |
| `endToEndId` ISO 20022 (32 chars: `E` + ISPB + `yyyyMMddHHmm` + 11-char UUID) | `domain/model/EndToEndId.generate()` |
| Status payment: `RCVD` → `PDNG` → `ACSP` → `ACSC|RJCT` | `domain/model/PaymentInitiation.java` + state machine |
| Pós-payment ACSP, consent vira CONSUMED | `CreatePaymentService` chama `consent.consume(now)` após `holderGateway.submit` aceitar |
| Settlement assíncrono | `HolderSimulatorController.submitPayment()` schedula `SettlePaymentUseCase.settle()` via `ScheduledExecutorService` |

## Segurança & compliance

| Spec | v0.1.0 | Roadmap |
|---|---|---|
| **mTLS ICP-Brasil** | Off por default (`ofpayments.mtls.enabled=false`); SSL Bundle pronto pra plugar cert real | Doc `make certs` na v0.x |
| **OAuth2 + FAPI Baseline** | **Mock auth** (Spring Security `permitAll`) | **v0.2.0** — FAPI Advanced + OAuth2 + DCR |
| **DPoP / sender-constrained tokens** | — | **v0.2.0** |
| **JWS detached signature em payloads** | — | v0.3.0 |
| **Idempotency-Key** em POSTs | Não | v0.x — necessário antes de produção |
| **Webhook receiver pra status callback** | — | v0.4.0 |

## PII & audit

- Toda CPF/CNPJ é mascarada em log/audit/exception via `Document.masked()` (formato `12***99`).
- `StructuredAuditLog` emite eventos `CONSENT_CREATED`, `CONSENT_AUTHORISED`, `PAYMENT_PENDING`, `PAYMENT_ACCEPTED`, `PAYMENT_SETTLED`, `PAYMENT_REJECTED`, `CONSENT_CONSUMED`, `HOLDER_ERROR`.
- Métricas Micrometer: `ofpayments.audit.events{kind, outcome}` — counter por tipo.

## Observabilidade

- `nfc.operation.duration` (do template do dict-client) **não existe aqui** — métricas são auto-emitidas via Micrometer pelo Spring (`http_server_requests_seconds_*`) e Resilience4j (`resilience4j_*`).
- Counters customizados:
  - `ofpayments.audit.events{kind, outcome}` — tudo que o audit log emite
  - `ofpayments.simulator.consents.registered` / `.authorised`
  - `ofpayments.simulator.payments.received` / `.settled`
  - `ofpayments.simulator.failures.injected` / `.jitter.applied`

## Fora de v0.1.0

- Conformance test contra suite oficial do Open Finance Brasil
- Suporte a Pix Automático (mês N+1, mês N+M) — cross-link com `pix-automatico-reference`
- Open Insurance Payments (escopo do `open-insurance-transmissor-reference`)
- Persistência durável (Postgres) — hoje é só in-memory, perde tudo no restart
