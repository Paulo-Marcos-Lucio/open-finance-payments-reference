# ADR 0006 — FAPI Advanced + DPoP (v0.2.0)

**Status:** Aceito (v0.2.0)
**Data:** 2026-04-28

## Contexto

A v0.1.0 entregou os endpoints PISP funcionando com **mock auth** (`permitAll`) — tudo aberto. Suficiente pra demonstrar fluxo de consent + payment, mas não defensável em produção. O perfil **FAPI 2.0 + Brasil profile** do Open Finance Brasil exige:

1. **OAuth2 + PKCE** com `private_key_jwt` ou MTLS pra client authentication
2. **Sender-constrained tokens** — token não vale se replay vier de máquina diferente
3. **Pushed Authorization Requests (PAR)** — request de autorização pré-pusheado, não query string
4. **JARM** — Authorization Response como JWT
5. **DCR (Dynamic Client Registration)** — clients se cadastram dinamicamente via JWKS
6. **JWS detached signature** em payloads sensíveis

Implementar tudo numa release significaria entregar nada por meses. Decisão: priorizar a peça **mais valiosa pra defesa em profundidade** (sender-constrained tokens) e documentar o resto como roadmap explícito.

## Decisão

v0.2.0 implementa **DPoP (RFC 9449)** completo end-to-end + um **mock authorization server** in-process pra que o repo rode com flow real de obtenção e uso de token sender-constrained, sem dependência externa.

### Escopo entregue

| Componente | Onde |
|---|---|
| `DPoPValidator` — valida proof JWT (assinatura, htm/htu/iat/jti, thumbprint binding) | `infrastructure/security/dpop/` |
| `DPoPNonceCache` — anti-replay TTL 2min via Caffeine | `infrastructure/security/dpop/` |
| `AccessTokenIntrospector` — valida JWT bearer + extrai `cnf.jkt` | `infrastructure/security/dpop/` |
| `MockAuthKeystore` + `TrustedJwkSetProvider` — geração + introspecção de signing key | `infrastructure/security/dpop/` |
| `MockAuthTokenController` — endpoint `/mock-auth/token` que emite token bound | `infrastructure/auth/` |
| `DPoPAuthenticationFilter` — filter Spring que valida proof + binding em cada request | `adapter/web/` |
| `WebSecurityConfig` — chain dual: profile `fapi` exige DPoP em PISP endpoints; sem `fapi` segue `permitAll` (back-compat com v0.1.0 IT) | `adapter/web/` |
| 8 unit tests cobrindo cenários do validator + 3 IT end-to-end (token + use + ataque de roubo de token) | `src/test/java/.../security` + `it/` |

### Trade-offs aceitos

- **`MockAuthKeystore` gera RSA fresh no startup.** Em produção real, o auth server roda em outro processo (Keycloak, Spring Authorization Server) com chave em HSM, expondo `/.well-known/jwks.json`. O `TrustedJwkSetProvider` foi escrito pra trocar fácil pra fetch remoto na v0.3.0.
- **Sem `private_key_jwt` ou MTLS client auth.** O `/mock-auth/token` aceita qualquer cliente que apresente DPoP proof. Suficiente pra exercitar binding cnf.jkt → request, mas não pra autorização sénior — se atacante roubar um proof + um nome de cliente, consegue token pra esse cliente.
- **Sem PAR.** O endpoint `/mock-auth/token` é equivalente a `client_credentials` grant, sem authorization code intermediário. Significa que não há flow de consent humano — adequado pra demo de DPoP, não pra fluxo Pix real.
- **Sem JARM.** Consequência direta de não ter authorization code flow.
- **Sem DCR.** Clients são "self-declared" via header `X-Client-Id`. Real BCB exige software statement assinado e endpoint `/oidc/register` com `software_statement` JWT.

### Decisões pequenas

- **Algoritmos suportados pro DPoP proof:** ES256 (recomendado FAPI), RS256, PS256. Outros (HS256, EdDSA) rejeitados explicitamente.
- **Freshness window:** ±60s. Permite jitter de relógio sem expor janela de replay perigosa.
- **JTI cache:** TTL 2min. Maior que freshness window, evita race com proof na borda.
- **htu canonicalization:** strip query + fragment + trailing slash. Evita falso negativo quando query params reorganizam.
- **Profile-based ativation:** `--spring.profiles.active=fapi` liga DPoP. Sem o profile, comportamento da v0.1.0 (permitAll). IT default não quebra.

## Consequências

### Positivas
- **Token roubado + replay de outra máquina = 401**. Defesa em profundidade real, exercitada em test (`replayedTokenFromAnotherKeyIsRejected`).
- **8 unit tests + 3 IT** cobrem todos os caminhos (proof válido, htm wrong, htu wrong, iat stale, jti replay, signature tampered, thumbprint mismatch).
- **ArchUnit não reclamou** — DPoP infrastructure ficou em `infrastructure/security/`, filter em `adapter/web/`. Hexagonal preservada.
- **Back-compat**: `EndToEndPaymentIT` (v0.1.0) continua passando porque ativa só `local,simulator` (sem `fapi`).

### Negativas
- **+8 classes Java + 1 dep nova (`nimbus-jose-jwt`)**. Curva de aprendizado pra contribuintes.
- **MockAuthKeystore gera key nova no startup** — token emitido antes de restart vira lixo. Aceitável pra demo, não pra prod.
- **Sem PAR/JARM/DCR**, repo ainda não é "BCB-conformance ready" — só demonstra DPoP corretamente.

## Próximos passos (roadmap)

| Versão | Componente | Esforço estimado |
|---|---|---|
| v0.3.0 | `private_key_jwt` client authentication (RFC 7523) | 1-2 dias |
| v0.3.0 | DCR endpoint (`/oidc/register`) com software_statement JWT | 1-2 dias |
| v0.4.0 | PAR endpoint (`/oauth2/par`) | 1-2 dias |
| v0.4.0 | JARM (Authorization Response como JWT) | 1 dia |
| v0.5.0 | JWS detached signature em payloads | 1-2 dias |
| v0.6.0 | Conformance test contra suite oficial Open Finance Brasil | 1 sprint |

## Alternativas consideradas

- **Adotar Spring Authorization Server inteiro** desde a v0.2.0 — rejeitado por escopo. Spring AS é projeto inteiro; integrar bem requer 1-2 sprints e domina a release. v0.2.0 foca em DPoP isolado.
- **MTLS sender-constrained em vez de DPoP** — também válido pelo BCB profile. DPoP foi escolhido porque (a) não exige cert ICP-Brasil em dev/test, (b) é mais novo e menos documentado em Java, então tem maior valor didático/referência, (c) DPoP funciona com SOFTWARE keys, MTLS exige HSM em produção séria.
