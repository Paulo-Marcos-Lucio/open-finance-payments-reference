# ADR 0001 — Arquitetura Hexagonal

**Status:** Aceito (v0.1.0)
**Data:** 2026-04-28

## Contexto

O Open Finance Payments tem várias dependências externas: banco detentor da conta (HTTP), audit/log estruturado, persistência (futura), provedor de tokens FAPI (futura), simulador local. Cada uma delas é candidata a ser trocada (por exemplo, in-memory → Postgres na v0.x.0; mock auth → FAPI Advanced na v0.2.0).

Sem fronteira clara, qualquer mudança de adapter quebra use cases. E o domain (state machines de Consent e Payment Initiation) é o ativo mais valioso — não pode ficar acoplado a Spring, Jakarta ou ao formato JSON de quem chama.

## Decisão

Aplicar **Hexagonal Architecture (Ports & Adapters)** com 4 camadas:

- `domain/` — modelos, exceções, ports. **Sem Spring, sem Jakarta, sem nada externo.** Apenas Java + JDK.
- `application/` — use cases. Depende **apenas** de `domain/`.
- `infrastructure/` — implementação dos ports out (HTTP gateway, in-memory repositories, audit, simulator).
- `adapter/web/` — entrada HTTP — controllers, DTOs, exception handler, security.

Validação automática via **ArchUnit** em `HexagonalArchitectureTest`. Roda no CI; PR com violação não merge.

## Consequências

**Positivas:**
- Domain testável sem Spring (`ConsentTest`, `PaymentInitiationTest` rodam em ms)
- Trocar `InMemoryConsentRepository` por implementação JPA na v0.x.0 não toca application/domain
- FAPI Advanced (v0.2.0) entra como decorator/gateway do `HolderConsentGateway` sem mexer use cases

**Negativas:**
- Mais camadas pra quem está chegando
- Records do Spring `@ConfigurationProperties` ficam em `infrastructure/config/`, não no domain — pequeno boilerplate a mais

## Alternativas consideradas

- **Camadas tradicionais (controller → service → repo):** acoplaria domain a Spring e a JSON de entrada. Rejeitado.
- **Modular monolith via Spring Modulith:** overkill pro escopo atual; revisitar quando tivermos múltiplos contextos (ex: Open Insurance + Open Finance no mesmo repo).
