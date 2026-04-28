# Changelog

Todas as mudanças relevantes deste projeto são documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o versionamento segue [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Unreleased]

## [0.1.0] - 2026-04-28

Primeira release pública. Implementação Java de referência para **Iniciador de Pagamento (PISP) do Open Finance Brasil — Fase 4**, com simulador in-process do banco detentor (autorizador) embarcado no mesmo Spring context.

### Added — Domínio

- `Consent` record com state machine (`RCVD → AUTHORISED → CONSUMED|REJECTED|REVOKED|EXPIRED`)
- `PaymentInitiation` record com state machine ISO 20022 (`RCVD → PDNG → ACSP → ACSC|RJCT`)
- `EndToEndId` ISO 20022 de 32 chars (`E` + ISPB + `yyyyMMddHHmm` + UUID truncado)
- `Subject`, `Document` (com `masked()`), `Account`, `Ispb`, `Amount` (BRL com 2 decimais)
- Exceptions tipadas: `ConsentNotFoundException`, `PaymentNotFoundException`, `InvalidConsentStateException`, `InvalidPaymentStateException`, `ConsentNotAuthorisedException`, `HolderUnavailableException`

### Added — Use cases (PISP-side)

- `CreateConsentService` — cria consent, registra no holder via gateway, salva, audit
- `GetConsentService` — leitura com **lazy expiration** (aplica `expireIfNeeded(now)`)
- `AuthoriseConsentService` — chamado pelo simulator; transita RCVD → AUTHORISED
- `CreatePaymentService` — verifica consent AUTHORISED, cria payment, submete pro holder, consome consent ao aceitar
- `GetPaymentService` — leitura simples
- `SettlePaymentService` — chamado pelo simulator async; transita ACSP → ACSC

### Added — Infrastructure

- `HolderConsentHttpGateway` com `@Retry` + `@CircuitBreaker` + `@RateLimiter` (grupo `holder-consent`)
- `HolderPaymentHttpGateway` com `@Retry` + `@CircuitBreaker` (grupo `holder-payment`, sem rate limit)
- `InMemoryConsentRepository` e `InMemoryPaymentRepository` (`ConcurrentHashMap`)
- `StructuredAuditLog` emitindo `ofpayments.audit.events{kind, outcome}` e log JSON

### Added — Simulator

- `HolderSimulatorController` (`/sim/holder/...`) implementa registrar consent, autorizar inline (mimic SCA), submeter payment com settlement assíncrono
- `failure-rate` injeta erro 500 e `jitter-{min,max}-ms` adiciona latência configurável
- Estado in-memory; perde no restart por design

### Added — Web facade

- `POST /open-banking/payments/v1/consents` — cria consent (PISP-side)
- `GET /open-banking/payments/v1/consents/{id}` — status do consent
- `POST /open-banking/payments/v1/pix/payments` — cria payment baseado em consent autorizado
- `GET /open-banking/payments/v1/pix/payments/{id}` — status do payment
- `GlobalExceptionHandler` mapeando exceptions de domínio em HTTP 4xx/5xx
- Mock auth (Spring Security `permitAll`) — FAPI Advanced é roadmap v0.2.0

### Added — Observabilidade

- Métricas via `ofpayments.audit.events`, `ofpayments.simulator.*` (counters)
- Spring/Micrometer auto-emitidos: `http_server_requests_seconds_*`, `resilience4j_*`
- Prometheus + Tempo + Loki + Grafana provisionados via compose.yaml

### Added — Qualidade & CI

- 14 unit tests (Consent + PaymentInitiation state machines + CreateConsentService + ArchUnit)
- 1 IT end-to-end (consent lifecycle + payment settlement) com simulator no mesmo Spring context
- ArchUnit valida hexagonal estrita (domain sem Spring/Jakarta, application só em domain)
- 7 jobs CI paralelos (build, unit, IT, ArchUnit, Semgrep, Trivy, Docker)
- CodeQL Java analysis, Dependency Review, Release tag-driven com SBOM CycloneDX

### Documentação

- 5 ADRs (`docs/adr/`): hexagonal, consent state machine, payment status (ISO 20022), resilience por grupo, simulator-as-controller
- Compliance mapping (`docs/compliance/of-payments-mapping.md`): cada feature da spec → onde está no código
- `requests.http` com fluxo end-to-end pra IntelliJ/VS Code REST Client

### Roadmap explícito (não nesta release)

- v0.2.0 — FAPI Advanced + DPoP + DCR
- v0.3.0 — JWS detached signature em payloads (RFC 7515 + ICP-Brasil cert)
- v0.4.0 — Webhook receiver pra status callback assíncrono
- Persistência durável (Postgres com partitioning)
- Idempotency-Key em todos os POSTs

[Unreleased]: https://github.com/Paulo-Marcos-Lucio/open-finance-payments-reference/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Paulo-Marcos-Lucio/open-finance-payments-reference/releases/tag/v0.1.0
