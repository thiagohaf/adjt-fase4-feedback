# Specs — Módulo 06: Relatórios periódicos (Lambda)

| Campo | Valor |
| --- | --- |
| **Módulo** | `06-relatorios` |
| **FRs** | FR-11, FR-17, FR-12 |
| **Spine** | AD-3, AD-6, AD-10, AD-12, AD-13, AD-16, AD-17, AD-18 |

Change OpenSpec: `relatorios-ses-s3-lambda-quarkus` →
`openspec/changes/relatorios-ses-s3-lambda-quarkus/specs/relatorio-periodico-ses-s3/spec.md`.

## Resumo dos cenários

| ID | Cenário |
| --- | --- |
| SPEC-11.1 | Semanal → média + qty/dia + qty/urgência |
| SPEC-11.1b | Semanal → HTML/PDF listam Descrição \| Urgência \| Data de envio |
| SPEC-11.2 | Janela semanal civil SP via `ocorrido_em` |
| SPEC-11.3 | Read-only; deployable `report` distinto |
| SPEC-17.1 | Diário → média + qty total + qty/urgência |
| SPEC-17.1b | Diário → HTML/PDF listam Descrição \| Urgência \| Data de envio |
| SPEC-17.2 | Mesma Lambda, dois `periodo` |
| SPEC-12.1 | E-mail HTML tipo + agregados + lista individual |
| SPEC-12.2 | PDF em `relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf` (agregados + lista) |
| SPEC-12.3 | Invoke manual = mesma janela do cron |
| SPEC-12.4 | `adminEmail` via config |
| SPEC-12.5 | Janela vazia ainda entrega e-mail + PDF |

## Envelope demo (conectividade)

Lambda report na VPC default com SG `feedbacks-report-lambda` (subnets públicas +
`allowPublicSubnet`); SG do RDS autoriza esse SG junto com o das tasks ECS. Não há
`FEEDBACKS_REPORT_VPC_ID` opcional nem Lambda fora de VPC na demo — ver
`openspec/specs/relatorio-periodico-ses-s3/spec.md` e `design.md` §1.
