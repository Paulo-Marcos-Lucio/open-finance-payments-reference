# ADR 0005 — Simulator como Controller no mesmo Spring context

**Status:** Aceito (v0.1.0)
**Data:** 2026-04-28

## Contexto

Pra testar o fluxo end-to-end (PISP cria consent → autoriza → cria payment → settlement), precisamos de um banco detentor que receba e processe as chamadas. Em produção é um banco real, com cert ICP-Brasil. Em dev/test, três opções:

1. **Mock framework (Mockito/WireMock):** estado em memória do mock, mas o mock não roda HTTP de verdade — não exercita resilience4j, não valida que o gateway HTTP funciona, não dá tracing real.
2. **Container Docker separado** (testcontainers + simulator app): exercita HTTP/resilience, mas IT fica lento, e dependency em Docker no CI complica.
3. **Controller no mesmo Spring context** ativado por profile: HTTP real, instância única, IT roda em ms, mesmo deploy artifact pode rodar local com simulator embutido.

## Decisão

Implementar `HolderSimulatorController` como `@RestController` montado em `/sim/holder/...` no mesmo Spring context. Sempre disponível (não condicional a profile) pra simplificar IT — em produção, só não bate ninguém nesse path.

Estado do simulator: in-memory (`Set<String> registeredConsents`), perde no restart por design.

`@DynamicPropertySource` no `AbstractIntegrationIT` configura `ofpayments.holder.base-url=http://localhost:18082/sim` pra que o `HolderConsentHttpGateway` faça chamada HTTP real → loopback no mesmo Spring → controller do simulator → service real → estado in-memory.

## Consequências

**Positivas:**
- IT end-to-end em ~7s (Spring Boot startup + 1 fluxo completo)
- Resilience4j é exercitado (annotations `@Retry`, `@CircuitBreaker` realmente disparam)
- `traceId` propaga PISP → simulator → settlement (single Spring context = single OpenTelemetry tracer)
- `make run-sim` deixa um setup local zero-friction pra demo no LinkedIn ou em call de venda

**Negativas:**
- Em produção real, o controller fica deployado mas nunca acessado (override em `/sim/...`). Filter de security ou env-flag remove a v0.x se virar incômodo.
- Não exercita comportamento de rede real (retry com socket timeout). Pra isso, há um path futuro com WireMock + jitter.

## Alternativas consideradas

- **Profile-conditional controller** (`@Profile("simulator")`): rejeitado pq complica IT — Spring não monta o bean, gateway HTTP bate em 404, falha com mensagem confusa
- **WireMock como `@TestConfiguration` bean:** considerar pra v0.x quando precisarmos exercitar timeouts de socket de forma determinística
- **Spring Cloud Contract:** overkill pra um repo de referência
