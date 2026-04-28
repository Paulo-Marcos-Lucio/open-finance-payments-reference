# Guia para Claude

Notas internas para sessões futuras de desenvolvimento assistido por IA. Não é documentação para usuários — para isso veja [README.md](./README.md) e [CONTRIBUTING.md](./CONTRIBUTING.md).

## O que este projeto é

Implementação Java de **referência** (não app pronto) para **Iniciador de Pagamento (PISP) do Open Finance Brasil — Fase 4** — fluxo de consent + payment initiation + simulador in-process do detentor de conta. Posicionamento é portfolio público de consultoria — qualquer mudança deve preservar/elevar o nível profissional do código e da documentação.

Faz parte da **Suíte de Referência Regulatória BR** mantida pelo Paulo SP, ao lado de `pix-automatico-reference`, `dict-client-reference` e `pix-nfc-reference`.

## Convenções inegociáveis

### Arquitetura hexagonal
- `domain/` é **puro**: sem Spring, sem Jakarta, sem nada externo. Só Java.
- `application/` depende **apenas** de `domain/`. Nunca importe de `infrastructure/` ou `adapter/`.
- `infrastructure/` implementa portas de saída (`domain/port/out/`).
- `adapter/web/` é o ponto de entrada HTTP — controllers, DTOs, filters, security.
- `HexagonalArchitectureTest` (ArchUnit) valida tudo isso no CI. Não burlar.

### State machines no domínio
- `Consent` e `PaymentInitiation` têm transições válidas codificadas no record (métodos como `authorise()`, `accept()`, `settle()`, `reject()`).
- Transições inválidas lançam `InvalidConsentStateException` / `InvalidPaymentStateException` — **NÃO** silenciosamente faz nada.
- Quem chama é responsável por persistir o resultado da transição.

### mTLS
- Cliente HTTP usa `SslBundle` resolvido pelo Spring quando `ofpayments.mtls.enabled=true`.
- Truststore aceita só ICP-Brasil em produção; em local/test é `enabled: false`.
- Nunca commitar certificados, chaves privadas, p12, jks. `.gitignore` bloqueia.
- Para dev local: `make certs` gera material de teste em `certs/` (gitignored).

### Mascaramento de PII
- Todo CPF/CNPJ em log/audit/exception **deve** sair mascarado. Helper `Document.masked()` é a forma canônica.
- Nunca logar `document.value()` direto. ArchUnit não pega isso — review humano sim.

### Simulator
- Ativado via profile `simulator`. Roda no mesmo Spring context da app pra IT end-to-end.
- Estado é in-memory (`Set<String> registeredConsents`) — perde tudo no restart, propositalmente.
- `ofpayments.simulator.failure-rate` injeta erro 500; `jitter-{min,max}-ms` adiciona latência. Pra exercitar resiliência client-side.

### Resilience4j
- 2 grupos: `holder-consent` (alta frequência, rate-limited, retry agressivo, 4 attempts) e `holder-payment` (retry conservador 2 attempts — Pix não tolera duplicação).
- Não decorar use cases, decorar adapters HTTP — resilience é responsabilidade de infra.

### Testes
- Unit: `*Test.java` em `src/test/java/...`. Domínio puro testado direto.
- Integration: `*IT.java`, herda de `AbstractIntegrationIT`. Sobe app inteiro com profiles `local,simulator` em port fixa 18082.
- Architecture: `HexagonalArchitectureTest`.
- Surefire roda `*Test.java` na fase `test`. Failsafe roda `*IT.java` na fase `verify`.
- IT usa `DEFINED_PORT=18082` (não RANDOM_PORT) pq holder e PISP rodam na MESMA instância — `ofpayments.holder.base-url` precisa apontar pro port real, e `@DynamicPropertySource` resolve isso.

### Gotchas conhecidas
- **`Map.of(k, null)` lança NPE em runtime** — usar `HashMap` ou `Map.ofEntries(...)` quando valores podem ser nulos. Ver `HolderSimulatorController.submitPayment` (`rejectionReason: null`).
- `OfPaymentsProperties` é `@ConfigurationProperties(prefix="ofpayments")` com sub-records. Spring Boot 3.4 suporta records, mas se algum sub-record não tiver entrada no yml, vem null e dá NPE. Manter todos os blocos no yml mesmo que com defaults.
- Chamar `consent.authorise(now)` quando ele já está AUTHORISED lança `InvalidConsentStateException`. Use case `AuthoriseConsentService` não tolera reautorização.

## Comandos frequentes

```bash
# Stack local (observabilidade)
make up
make down

# Build / testes
make test     # unit (14 tests verdes em ~5s)
make it       # unit + integration (15 tests, IT em ~7s)

# Run app
make run        # profile local (sem simulator, vai bater contra holder real configurado)
make run-sim    # profile local + simulator (holder no mesmo Spring context na port 8082)
make load       # tráfego sintético end-to-end pra alimentar dashboards

# Imagem OCI via Buildpacks
make image
```

## Operações no GitHub via gh

`gh` é autenticado via env var puxando o PAT do Git Credential Manager:
```bash
export GH_TOKEN=$(printf "protocol=https\nhost=github.com\n\n" | git credential fill 2>/dev/null | sed -n 's/^password=//p' | head -1)
```
Token tem scopes `repo`, `workflow`, `gist`. Suficiente pra tudo no repo.

```bash
gh run list -R Paulo-Marcos-Lucio/open-finance-payments-reference --limit 5 -w CI
gh run watch <run-id> -R Paulo-Marcos-Lucio/open-finance-payments-reference --exit-status
```

## Branch protection

`main` é protegida (após primeiro push): status checks `CI status` obrigatório, linear history, sem force push, sem delete. Mudanças entram via PR squash-merged.

## Release

Tag `v*.*.*` dispara `release.yml`: builda imagem OCI, push pra `ghcr.io/Paulo-Marcos-Lucio/open-finance-payments-reference`, cria GitHub Release com SBOM CycloneDX e Trivy SARIF.

## Fora de escopo (não fazer sem pedido)

- Conexão real com instituição autorizada do Open Finance em produção (precisa software statement + ICP-Brasil emitido)
- Persistência durável (Postgres) — hoje é só in-memory
- FAPI Advanced + DPoP + DCR — roadmap explícito v0.2.0
- JWS detached signature — roadmap v0.3.0
- Webhook receiver — roadmap v0.4.0

## Mensagens de commit

PT-BR informal, claro e elucidativo. Mantém prefixo Conventional Commits (`fix(escopo):`, `feat(escopo):`, etc.) mas texto em português. Explica *o que* mudou e *por quê*.

```
fix(simulator): trocar Map.of por HashMap quando rejectionReason é null

Map.of lança NPE com null values. O simulator do holder retorna
{accepted: true, rejectionReason: null} no caminho feliz, então
batia em runtime. HashMap aceita null sem reclamar.
```

## Memória do harness

Arquivos em `~/.claude/projects/.../memory/` documentam preferências do Paulo (autonomia, comunicação, posicionamento profissional, suíte de repos). Atualizar quando aprender algo durável; não duplicar conteúdo do código.
