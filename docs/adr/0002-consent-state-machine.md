# ADR 0002 — State machine de Consent

**Status:** Aceito (v0.1.0)
**Data:** 2026-04-28

## Contexto

A spec do Open Finance Brasil define ciclo de vida do consent:

```
RCVD → AUTHORISED → CONSUMED
RCVD → REJECTED
RCVD → EXPIRED
AUTHORISED → REVOKED
```

Implementar isso ad-hoc com `consent.setStatus(...)` espalhado em controllers/services é receita pra bugs regulatórios — em produção, uma transição inválida (ex: pular pra CONSUMED sem AUTHORISED) viola compliance e pode ser flag de fraude.

## Decisão

Modelar o ciclo de vida como **state machine no domínio**, dentro do record `Consent`. Cada transição é um método explícito que retorna **novo** Consent (record imutável):

```java
public Consent authorise(Instant now)   // RCVD → AUTHORISED
public Consent reject(Instant now)      // RCVD|AUTHORISED → REJECTED
public Consent consume(Instant now)     // AUTHORISED → CONSUMED
public Consent revoke(Instant now)      // RCVD|AUTHORISED → REVOKED
public Consent expireIfNeeded(Instant)  // RCVD → EXPIRED se now > expiresAt
```

Transição inválida lança `InvalidConsentStateException` (extends `PaymentsException`). Quem chama é responsável por persistir o resultado.

A expiração é **lazy** (calculada em leitura) em vez de via scheduled job — `GetConsentService` roda `expireIfNeeded(now)` antes de retornar.

## Consequências

**Positivas:**
- Cada transição testada isoladamente em `ConsentTest` (6 tests cobrindo paths válidos e inválidos)
- Application services não precisam validar transições — domain enforce
- Lazy expiration evita race conditions com payments criados durante "limbo"

**Negativas:**
- Cada transição envolve `repository.save(novoConsent)` — se esquecer, estado não persiste. Mitigado por code review e por tests de application service (ex: `CreateConsentServiceTest` checa que repositório recebe save)
- Cliente pode receber um EXPIRED após GET mesmo que tenha sido criado RCVD — esperado, é o comportamento correto

## Alternativas consideradas

- **Spring Statemachine:** overkill, traz dependency pesada. Rejeitado.
- **Enum + tabela de transições:** mais data-driven mas menos legível. Rejeitado.
- **Mutate field + setter:** breaks immutability, mata thread-safety. Rejeitado.
