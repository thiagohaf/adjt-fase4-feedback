# Proposal — Módulo 05: Alerta SES (Lambda)

| Campo | Valor |
| --- | --- |
| **Módulo** | `05-alerta-ses` |
| **PRD** | §4.5 Urgência / Notificação imediata ALTA |
| **FRs vinculados** | FR-10 (+ consumo AD-4/AD-5; publish já no módulo 04) |
| **User Journeys** | UJ-3 (Bruno — alerta no e-mail) |
| **Status** | Rascunho para revisão |
| **Dependências** | Módulo 04 (Avaliação + `EvaluationEventPublisher` / AD-5) |
| **Bloqueia** | Cena 8 do roteiro; evidência SM-1 de alerta |

---

## 1. Problema

Após Avaliação ALTA, a API publica (ou prepara) o evento AD-5, mas **ninguém consome** o
evento para e-mail. Não há `lambda-notification`, SES binding nem fila operacional. Bruno
não recebe Alerta de urgência sem consultar a API — UJ-3 e FR-10 falham na demo.

## 2. Valor de negócio

| Stakeholder | Valor entregue |
| --- | --- |
| **Administrador (Bruno)** | E-mail imediato com descrição, urgência e data quando nota ≤4. |
| **Desenvolvedor** | Lambda Quarkus SRP + contrato AD-5; caminho SQS→SES narrável no vídeo. |
| **Produto** | Fecha UJ-3; desbloqueia narrativa “API container + serverless alerta”. |

**Métricas / roteiro:** SM-1 (alerta ALTA); cena 8 (caixa de e-mail).

## 3. Escopo

### 3.1 In scope (MVP)

| # | Capacidade | Detalhe |
| --- | --- | --- |
| 1 | **Lambda notification** | Quarkus + `quarkus-amazon-lambda`; SQS → parse AD-5 → SES. |
| 2 | **E-mail alerta** | Destinatário `adminEmail`; corpo com descrição, urgência, data (`ocorridoEm`). |
| 3 | **Sem RDS** | Lambda só usa payload; fora da VPC (AD-17). |
| 4 | **Resiliência** | Retry SQS/Lambda + DLQ; falha SES não apaga Avaliação (já commitada). |
| 5 | **Adapter SQS API** | `SqsEvaluationEventPublisher` real quando queue URL configurada. |
| 6 | **CDK mínimo** | Fila + DLQ + Lambda + IAM SES + secret `adminEmail`. |
| 7 | **Testes** | Unitários parse/envio; JaCoCo ≥ 90% no app notification. |

### 3.2 Out of scope (MVP)

| Item | Motivo |
| --- | --- |
| Relatórios / `lambda-report` | FR-11+ |
| ECS / ECR / CI deploy completo | FR-15 restante |
| Health DB / métricas CloudWatch API | FR-13/14 |
| Alterar domínio Avaliação / Urgência | Já no módulo 04 |
| SES production access | Sandbox OK |

### 3.3 Premissas

- Publish ALTA já filtra MÉDIA/BAIXA — Lambda só trata mensagens ALTA na fila.
- Mesmo JSON AD-5 em Kafka local e SQS AWS.
- Um Administrador de demo; e-mail verificado no SES.

## 4. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| SES sandbox bloqueia destinatário | Documentar verificação do endereço; falha visível em log/DLQ |
| SQS ainda não provisionado no CI | Testes unitários com fake SES; CDK synth local |
| Duplicar contrato AD-5 | Fixture JSON única; campos idênticos ao record da API |
| Escopo CDK crescer para FR-15 | Só stack de alerta; sem ECS/RDS |

## 5. Entregáveis

1. App `feedbacks/apps/notification` + `function.zip`.
2. CDK mínimo do caminho alerta.
3. Adapter SQS da API operacional (config-driven).
4. Testes + JaCoCo ≥ 90%.
5. Docs OpenSpec do módulo + change `alerta-ses-lambda-quarkus`.

## 6. Critério de pronto do módulo

- [ ] Mensagem AD-5 válida → SES chamado com descrição, urgência e data.
- [ ] Payload inválido → não envia e-mail; falha observável (log / exception → retry/DLQ).
- [ ] Lambda não acessa banco.
- [ ] `adminEmail` via config/secret — não hardcoded no código.
- [ ] JaCoCo ≥ 90% no módulo notification.
- [ ] MÉDIA/BAIXA continuam sem publish (regressão módulo 04).
- [ ] Auth, catálogo, inscrição, Avaliação HTTP inalterados.
