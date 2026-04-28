# open-finance-payments-reference

> Implementação de **referência** *production-grade* do **Iniciador de Pagamento (PISP)** do
> Open Finance Brasil — fluxo de consentimento, criação de pagamento Pix iniciado por
> terceiro, simulador in-process do banco autorizador (detentor de conta), arquitetura
> hexagonal validada por ArchUnit, observabilidade end-to-end. mTLS-ready (ICP-Brasil) e
> roadmap para FAPI Advanced + DPoP em v0.2.0.
>
> Java 21 · Spring Boot 3.4 · Hexagonal · Resilience4j · OpenTelemetry · Grafana

[![CI](https://github.com/Paulo-Marcos-Lucio/open-finance-payments-reference/actions/workflows/ci.yml/badge.svg)](https://github.com/Paulo-Marcos-Lucio/open-finance-payments-reference/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)

---

## Por que este repo existe

O **Open Finance Brasil — Fase 4 (Iniciação de Pagamento)** permite que um TPP (Third Party
Provider, atuando como PISP) inicie um pagamento Pix em nome do usuário, contra o banco
detentor da conta — sem o usuário sair do app do iniciador. É o *Pix iniciado por
terceiro* — fundamento de fluxos de checkout one-tap, transferência por aproximação, e
recargas em apps de mobilidade/varejo.

Spec é vasta (consent flow + payment flow + FAPI + DCR + ICP-Brasil + JWS sigs) e o
material open source de referência em Java é fragmentado. Este repo cobre o **núcleo do
fluxo** com nível de produção e roadmap explícito pros componentes que ainda faltam
(FAPI/DPoP/DCR em v0.2.0).

Faz parte da **Suíte de Referência Regulatória BR** mantida ao lado de:

- [`pix-automatico-reference`](https://github.com/Paulo-Marcos-Lucio/pix-automatico-reference) — Pix Automático + Open Finance Fase 4 com saga, outbox, painel React
- [`dict-client-reference`](https://github.com/Paulo-Marcos-Lucio/dict-client-reference) — cliente DICT do BCB
- [`pix-nfc-reference`](https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference) — Pix por aproximação (NFC)

## O que está aqui (v0.1.0 — MVP)

- **Lado iniciador (PISP):** criação de consent, consulta de status, criação de payment
  baseado em consent autorizado, consulta de payment com cadeia completa de status
- **Lado detentor (simulator):** endpoints HTTP que mimetizam o banco autorizador —
  registra consents, autoriza inline (mimics customer SCA approval), recebe pagamentos
  e agenda settlement assíncrono
- **State machines do domínio:**
  - Consent: `RCVD` → `AUTHORISED` → `CONSUMED` | `REJECTED` | `REVOKED` | `EXPIRED`
  - Payment Initiation: `RCVD` → `PDNG` → `ACSP` → `ACSC` | `RJCT`
- **EndToEndId ISO 20022** de 32 chars com prefixo `E` + ISPB do debtor + timestamp + UUID
- **Resilience4j** em 2 grupos: `holder-consent` (rate-limited, retry agressivo) e
  `holder-payment` (retry conservador 2 attempts pra evitar duplicar Pix)
- **Audit log estruturado JSON** com documento sempre mascarado (`Document.masked()`)
- **Observabilidade rica:** Prometheus + Tempo + Loki, OpenTelemetry traces correlacionados
  por `traceId`, exemplars Tempo no histograma de latência
- **Hexagonal estrito + ArchUnit** — domain puro (sem Spring, sem Jakarta), application
  só depende de domain, infrastructure implementa ports out
- **mTLS ICP-Brasil-ready** via Spring Boot SSL Bundle (config-driven, off por default)
- **CI/CD end-to-end:** 7 jobs paralelos GitHub Actions (build, unit, IT, ArchUnit,
  Semgrep SAST, Trivy image scan, Docker) + CodeQL + Dependency Review + Release tag-driven

## Quickstart

```bash
git clone https://github.com/Paulo-Marcos-Lucio/open-finance-payments-reference
cd open-finance-payments-reference

make up           # otel + prometheus + tempo + loki + grafana
make run-sim      # app + simulador holder in-process
make load         # tráfego sintético end-to-end
```

Acesse:
- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **Grafana (admin/admin):** http://localhost:3000
  - Dashboard **OF Payments · Operations Overview**
  - Dashboard **OF Payments · Resilience**
- **Prometheus:** http://localhost:9090

## Endpoints PISP

```http
POST /open-banking/payments/v1/consents                  # cria consent
GET  /open-banking/payments/v1/consents/{consentId}      # status do consent
POST /open-banking/payments/v1/pix/payments              # cria payment (consent precisa estar AUTHORISED)
GET  /open-banking/payments/v1/pix/payments/{paymentId}  # status do payment
```

## Endpoints simulator (banco detentor in-process)

```http
POST /sim/holder/consents                                # PISP registra consent no holder
POST /sim/holder/consents/{consentId}/authorise          # autoriza inline (mimic customer SCA)
POST /sim/holder/payments                                # PISP envia payment; settlement assíncrono
```

Veja [`requests.http`](./requests.http) pra exemplos rodáveis na IntelliJ ou via VS Code REST Client.

## Arquitetura

Camadas (validadas por **ArchUnit** em `HexagonalArchitectureTest`):

- `domain/` — modelos, exceções, ports, state machines. **Sem Spring, sem Jakarta**
- `application/` — use cases (CreateConsent, GetConsent, AuthoriseConsent, CreatePayment,
  GetPayment, SettlePayment). Depende **apenas** de `domain/`
- `infrastructure/` — implementação dos ports out (HTTP gateway, in-memory repositories,
  audit, simulator)
- `adapter/web/` — controllers HTTP, DTOs, exception handler, security config

## Padrões de implementação destacados

| Padrão | Onde aplica | Por que importa |
|---|---|---|
| **State machines no domínio** | `Consent`, `PaymentInitiation` | Transições válidas explícitas; ArchUnit + tests evitam regressão |
| **Documents mascarados por default** | `Document.masked()`, `Subject.maskedDocument()` | Logs/audit nunca expõem CPF/CNPJ completo |
| **Resilience por grupo de operação** | `holder-consent` vs `holder-payment` | Consent tolera retry agressivo; payment não — Pix duplicado é incidente regulatório |
| **Simulator-as-controller** | `HolderSimulatorController` | IT end-to-end no mesmo Spring context, sem Docker, sem WireMock |
| **EndToEndId ISO 20022** | `EndToEndId.generate()` | 32 chars exatos, prefixo `E` + ISPB + timestamp + UUID truncado |
| **Hexagonal estrito + ArchUnit** | `HexagonalArchitectureTest` | Garantido em CI — não dá pra acidentalmente importar Spring no domain |
| **Sender-constrained mTLS** *(produção)* | Spring SSL Bundle | Cert ICP-Brasil obrigatório em produção; off em local/test |

## Configuração

Todos os parâmetros via `ofpayments.*` em `application.yml` ou env vars correspondentes.

```yaml
ofpayments:
  initiator:
    organisation-id: 11111111-1111-1111-1111-111111111111
    software-statement-id: 22222222-2222-2222-2222-222222222222
  holder:
    base-url: ${HOLDER_BASE_URL}
    connect-timeout: 2s
    read-timeout: 5s
  consent:
    default-validity-minutes: 5
    max-validity-minutes: 1440
  payment:
    settle-delay-ms: 250
  simulator:
    failure-rate: 0.0
    jitter-min-ms: 0
    jitter-max-ms: 0
  mtls:
    enabled: true
    bundle-name: ofpayments-prod
```

## Roadmap

- [x] v0.1.0 — Consent flow + Payment Initiation + state machines + simulator + observabilidade
- [ ] **v0.2.0 — FAPI Advanced + DPoP + DCR (Dynamic Client Registration)** *(prioridade)*
- [ ] v0.3.0 — JWS detached signature em payloads (ICP-Brasil cert + RFC 7515)
- [ ] v0.4.0 — Webhook receiver pra status callback assíncrono do holder
- [ ] v0.5.0 — Suporte a Open Insurance Payments e Pix Automático cross-link
- [ ] Persistência durável (Postgres com partitioning) — hoje é in-memory
- [ ] Idempotência forte com `Idempotency-Key` em todos os POSTs
- [ ] Conformance test contra a suite oficial do Open Finance Brasil

## Compliance

ADRs em `docs/adr/`:

- [0001 — Hexagonal architecture](docs/adr/0001-hexagonal-architecture.md)
- [0002 — Consent state machine](docs/adr/0002-consent-state-machine.md)
- [0003 — Payment status mapping (ISO 20022)](docs/adr/0003-payment-status-mapping.md)
- [0004 — Resilience strategy por grupo](docs/adr/0004-resilience-strategy.md)
- [0005 — Simulator-as-controller no Spring context](docs/adr/0005-simulator-as-controller.md)

## Licença

[MIT](LICENSE) — use, modifique, distribua. Atribuição apreciada.

## Autor

**Paulo Marcos Lucio** — Engenheiro Java pleno · Consultor em integrações regulatórias BR

[LinkedIn](https://www.linkedin.com/in/paulo-marcos-a07379174/) ·
[GitHub](https://github.com/Paulo-Marcos-Lucio) ·
pmlsp23@gmail.com

> Se este repo ajudou seu time, ⭐ uma star — ajuda outros engenheiros do nicho a encontrarem.
