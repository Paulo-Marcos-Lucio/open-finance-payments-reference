# ADR 0004 — Resilience strategy por grupo de operação

**Status:** Aceito (v0.1.0)
**Data:** 2026-04-28

## Contexto

O TPP (PISP) chama dois endpoints distintos no banco detentor:

1. **Consent flow** — registra consents, alta frequência, idempotente do lado do holder. Se a chamada falhar, retentar é seguro (mesmo consent_id, mesmo body — holder retorna o registro existente ou aceita de novo)
2. **Payment flow** — submete um pagamento. **Não-idempotente sem cuidado**: se a chamada falhar com timeout no meio, o detentor pode ter recebido e criado o Pix. Retentar duplicaria.

Resilience4j permite definir grupos com config diferente. Aplicar mesma política nos dois é antipattern: ou retry agressivo demais (duplica Pix) ou conservador demais (perde consents por flap de rede).

## Decisão

**Dois grupos** Resilience4j configurados em `application.yml`:

```yaml
resilience4j:
  retry:
    instances:
      holder-consent:
        max-attempts: 4          # agressivo
        wait-duration: 100ms
      holder-payment:
        max-attempts: 2          # conservador
        wait-duration: 200ms
  circuitbreaker:
    instances:
      holder-consent:
        sliding-window-size: 50
        failure-rate-threshold: 50%   # tolerante
      holder-payment:
        sliding-window-size: 50
        failure-rate-threshold: 30%   # sensível, abre cedo
  ratelimiter:
    instances:
      holder-consent:
        limit-for-period: 200
        limit-refresh-period: 1s
        timeout-duration: 100ms
  # NÃO há rate limiter pra holder-payment — payment é raw user action, não sintético
```

Aplicado nos gateways via anotações `@Retry`, `@CircuitBreaker`, `@RateLimiter`.

## Consequências

**Positivas:**
- Risco de duplicação de Pix limitado a max 2 attempts (e ainda assim, idempotency key futura — v0.x — vai tornar safe)
- Consent flow tolera saturação intermitente do detentor sem quebrar a UX do iniciador
- Métricas Resilience4j separadas em Prometheus (`resilience4j_circuitbreaker_state{name="holder-consent"}` vs `holder-payment`) — dashboard `OF Payments · Resilience` mostra cada grupo

**Negativas:**
- Mais config pra entender
- Cuidado no roadmap v0.4.0 (webhook receiver) pra não tratar callback assíncrono como retry-safe

## Alternativas consideradas

- **Política única**: rejeitado por motivos óbvios
- **3 grupos** (separar `holder-consent-write` de `holder-consent-read`): pra v0.1.0 exagero. Considerar quando tivermos GET de status batido em loop pelo cliente
