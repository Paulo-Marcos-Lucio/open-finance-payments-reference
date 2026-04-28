# ADR 0003 — Mapeamento de PaymentStatus para ISO 20022

**Status:** Aceito (v0.1.0)
**Data:** 2026-04-28

## Contexto

O Open Finance Brasil herda terminologia do ISO 20022 (`pacs.002.001.13`) pro ciclo de vida de um pagamento:

| Code | Meaning | When |
|---|---|---|
| `RCVD` | Received | TPP recebeu a request, ainda não enviou pro detentor |
| `PDNG` | Pending | Enviou pro detentor, aguardando processamento |
| `ACSP` | Accepted Settlement in Process | Detentor aceitou e está liquidando |
| `ACSC` | Accepted Settlement Completed | Liquidação concluída, dinheiro creditado |
| `RJCT` | Rejected | Rejeitado em qualquer estágio |

A spec do Open Finance Brasil v4 alinha com isso, e a [pacs.002.001.13 do ISO 20022](https://www.iso20022.org/) é o padrão pra mensageria SPI.

## Decisão

Usar **exatamente os códigos ISO 20022** como nome do enum `PaymentStatus`. Sem renaming pra "mais legível em português" — quem trabalha com pagamentos sabe o que ACSP significa, e usar `ACCEPTED_SETTLEMENT_IN_PROCESS` espalha verbosity pelo domínio sem ganho.

State machine implementada no record `PaymentInitiation`:

```
RCVD → PDNG → ACSP → ACSC
RCVD|PDNG → RJCT (caminho de erro, com rejectionReason)
```

API HTTP retorna o enum como string direta no JSON: `"status": "ACSP"`. Cliente que não conhece ISO 20022 lê os 5 códigos no README e na tabela acima.

## Consequências

**Positivas:**
- Engenheiros fluentes em ISO 20022 reconhecem imediatamente
- Quando virmos `pacs.008.001.10` (mensageria real do SPI) na v0.3.0, fica trivial mapear
- Logs estruturados ficam interoperáveis com SIEMs que indexam por status code padrão

**Negativas:**
- Onboarding mais lento pra quem não conhece
- Mitigado: README documenta os 5 códigos; tabela acima fica fixa em `docs/`

## Alternativas consideradas

- Nomes traduzidos (`RECEBIDO`, `PENDENTE`, `ACEITO_PROCESSANDO`, etc.) — rejeitado, perde alinhamento com a spec
- Nomes em inglês expandido (`RECEIVED`, `PENDING`, etc.) — rejeitado, ainda perde a coincidência exata com ISO
