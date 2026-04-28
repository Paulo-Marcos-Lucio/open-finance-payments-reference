# Política de Segurança

## Versões suportadas

Como este é um projeto de referência sob desenvolvimento ativo, **apenas a `main` recebe correções de segurança**. Releases anteriores são marcadas como obsoletas.

| Versão | Suportada |
|---|---|
| `main` (HEAD) | ✅ |
| `v0.x.x` | ⚠️ apenas via novas releases |

## Reportando uma vulnerabilidade

**Não abra issue pública para vulnerabilidades.** Use o canal privado de Security Advisories do GitHub:

👉 https://github.com/Paulo-Marcos-Lucio/open-finance-payments-reference/security/advisories/new

Ao reportar, inclua:

- Descrição do problema e impacto potencial
- Passos para reproduzir (PoC se possível)
- Versão / commit afetado
- Sugestão de correção, se houver

### O que esperar

| Etapa | SLA alvo |
|---|---|
| Confirmação de recebimento | até 3 dias úteis |
| Avaliação inicial e severidade | até 7 dias úteis |
| Patch e advisory público | de acordo com a severidade |

Severidade segue [CVSS 3.1](https://www.first.org/cvss/calculator/3.1).

## Escopo

Vulnerabilidades de interesse:

- Bypass de mTLS / aceitação indevida de certificado fora da cadeia ICP-Brasil
- Vazamento de PII em logs (CPF, CNPJ, e-mail em claro — todo `Document` deve sair `masked()`)
- Race condition em transição de Consent (ex: forçar CONSUMED sem AUTHORISED)
- Duplicação de payment via retry agressivo no `holder-payment` group
- Bypass de autenticação / autorização no facade
- Configurações inseguras de Spring Security
- Dependências vulneráveis com exploit conhecido
- Cross-site request forgery em endpoints PISP
- Injeção (XML / HTTP header / log)
- Deserialização insegura

Fora de escopo (mas reporte mesmo assim):

- Engenharia social / phishing
- Ataques de negação de serviço (DoS) sem amplificação
- Vulnerabilidades em dependências sem caminho exploitável demonstrado
- Comportamento do simulator local (não é destinado a produção; em produção real, o controller fica deployado mas inacessível por filter de path)

## Disclosure

Trabalhamos em modelo de **disclosure coordenado**: após o patch, o advisory é publicado com crédito ao reporter (a menos que prefira anonimato).

## Hardening default

Esta implementação já aplica:

- mTLS configurável via `SslBundle` (em produção: obrigatório, ICP-Brasil)
- Logs estruturados sem PII — documentos sempre mascarados (`12***99`)
- State machines no domínio impedem transições inválidas (`InvalidConsentStateException`, `InvalidPaymentStateException`)
- Resilience4j separa rate limiter / retry policy entre `holder-consent` (agressivo) e `holder-payment` (conservador, anti-duplicação)
- Headers de segurança via Spring Security
- Dependency scanning contínuo (Dependabot + CodeQL + Trivy + Semgrep)
- Validação de payload via Jakarta Bean Validation no facade
- ArchUnit valida fronteira hexagonal — `domain/` não pode importar Spring/Jakarta

## Reconhecimentos

Lista de pesquisadores que reportaram vulnerabilidades válidas será mantida em `SECURITY-HALL-OF-FAME.md` (ainda vazio).
