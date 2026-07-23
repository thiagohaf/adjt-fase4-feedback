# Proposal — Módulo 06: Relatórios periódicos (Lambda)

| Campo | Valor |
| --- | --- |
| **Módulo** | `06-relatorios` |
| **PRD** | §4.6 Relatórios periódicos (serverless) |
| **FRs vinculados** | FR-11, FR-17, FR-12 |
| **User Journeys** | UJ-4 (Bruno — e-mail HTML + PDF S3) |
| **Status** | Em proposta (change `relatorios-ses-s3-lambda-quarkus`) |
| **Dependências** | Módulo 04 (Avaliação persistida); módulo 05 opcional para SES verificado |
| **Bloqueia** | Cenas 9–10 do roteiro; evidência SM-1 de relatório semanal + PDF |

---

## 1. Problema

Há Avaliações e alerta ALTA, mas **não há** consolidação periódica: sem
`lambda-report`, EventBridge, agregação RDS, e-mail HTML de relatório nem PDF no S3.
UJ-4 e FR-11/12/17 falham na demo.

## 2. Valor de negócio

| Stakeholder | Valor entregue |
| --- | --- |
| **Administrador (Bruno)** | E-mail diário/semanal com agregados + PDF no S3. |
| **Desenvolvedor** | Segunda Lambda SRP (Quarkus) + EventBridge + S3 narráveis. |
| **Produto** | Fecha UJ-4; enunciado ≥2 serverless + relatório semanal. |

## 3. Escopo

### 3.1 In scope (MVP)

| # | Capacidade | Detalhe |
| --- | --- | --- |
| 1 | **Lambda report** | Quarkus; `periodo=diario\|semanal`; VPC + RDS read-only. |
| 2 | **Agregados** | Diário/semanal conforme AD-16; janelas civis SP (AD-6). |
| 3 | **HTML + PDF** | SES + S3 key canônica. |
| 4 | **EventBridge + invoke** | Crons 08:00 SP; demo via invoke (AD-10). |
| 5 | **CDK** | Stack relatório (S3, Lambda, rules, IAM/secrets). |
| 6 | **Testes** | Unitários janela/agregação/entrega; JaCoCo ≥ 90%. |

### 3.2 Out of scope (MVP)

| Item | Motivo |
| --- | --- |
| Endpoint HTTP de relatório | AD-10 |
| ECS / ECR / CI full | FR-15 |
| Health / métricas API | FR-13/14 |
| Misturar com `lambda-notification` | SRP |

### 3.3 Premissas

- Avaliações já persistem `nota`, `urgencia`, `ocorrido_em`.
- Um Administrador de demo; SES sandbox OK.
- VPC/RDS demo disponíveis ou importáveis no CDK.

## 4. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| Sem RDS no CDK atual | Props importáveis + fake read model no CI |
| TZ / janela errada | Testes de fronteira SP |
| SES sandbox | Reusar `adminEmail` verificado |

## 5. Entregáveis

1. App `feedbacks/apps/report` + `function.zip`.
2. CDK do caminho relatório.
3. Testes + JaCoCo ≥ 90%.
4. Docs OpenSpec módulo 06 + change `relatorios-ses-s3-lambda-quarkus`.

## 6. Critério de pronto do módulo

- [ ] Invoke `diario` → HTML + PDF com agregados do dia civil anterior.
- [ ] Invoke `semanal` → HTML + PDF com média, qty/dia e qty/urgência.
- [ ] Crons EventBridge documentados (08:00 SP).
- [ ] Sem mutação de domínio; JaCoCo ≥ 90%; alerta/API intactos.
