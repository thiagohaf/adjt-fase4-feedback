# Specs — Módulo 05: Alerta SES (Lambda)

| Campo | Valor |
| --- | --- |
| **Módulo** | `05-alerta-ses` |
| **FRs** | FR-10 |
| **Spine** | AD-4, AD-5, AD-7, AD-12, AD-13, AD-17, AD-18 |

Change OpenSpec: `alerta-ses-lambda-quarkus` →
`openspec/changes/alerta-ses-lambda-quarkus/specs/alerta-notificacao-ses/spec.md`.

## Resumo dos cenários

| ID | Cenário |
| --- | --- |
| SPEC-10.1 | Payload ALTA válido → SES 1× com descrição, urgência, data |
| SPEC-10.2 | Lambda não acessa RDS |
| SPEC-10.3 | Destinatário via config/secret `adminEmail` |
| SPEC-10.4 | Body inválido → não chama SES; falha observável |
| SPEC-10.5 | Falha SES → retry/DLQ; sem mutação de Avaliação |
| SPEC-10.6 | Deployable `notification` separado da API (SRP) |

## Relação com publish (módulo 04)

| ID prep. | Comportamento (já entregue) |
| --- | --- |
| SPEC-10prep.1–5 | ALTA publica AD-5; MÉDIA/BAIXA não; falha publish não reverte |

FR-10 = prep. (API) **+** consumo SES (este módulo).
