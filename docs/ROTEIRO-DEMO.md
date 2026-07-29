# Roteiro de demonstração (YouTube) — FR-16 / PRD §11

Tempo-alvo: **8–15 minutos**. Ordem sugerida para não se perder na gravação.

**Pré-requisitos AWS (demo):** RDS `feedbacks-demo` available; EventBridge
`feedbacks-report-*` ENABLED; stack `FeedbacksApiStack` deployed (ALB);
SES sandbox com e-mail verificado; secret `feedbacks/jwt` + `feedbacks/db`.

**Ciclo de vida (Actions → Run workflow):**
- **`deploy(all)`** — sobe/retoma tudo (recria RDS+secrets se destroy apagou; 3 stacks + crons + seed + smoke)
- **`pause(demo)`** — corta custo horário (crons off, destroy API/ALB, stop RDS)
- **`destroy(all)`** — apaga stacks (e opcionalmente RDS/secrets); confirme com `destroy`

Atualize o `baseUrl` do Postman com o `ApiAlbDns` do summary do `deploy(all)`.

ALB demo (atualizar se redeploy — output `ApiAlbDns`):  
`http://Feedba-ApiSe-hGCfix1THZyT-1147675187.us-east-1.elb.amazonaws.com`  
Health: `/api/v1/health` · Ready: `/q/health/ready` · Alarme: `feedbacks-api-target-5xx` (SNS → e-mail admin)

## Checklist pré-gravação

- [ ] SES: identidade verificada; checar **Spam** no Gmail
- [ ] SNS: subscription do tópico do alarme 5XX **confirmada** no e-mail admin (uma vez)
- [ ] Seeds de usuários (Flyway) + seed relatório (`seed-report-demo.sql`) se UJ-4
- [ ] Postman: collection + env AWS ALB em `docs/postman/` (selecionar environment **AWS ALB**)
- [ ] Um PDF de relatório no S3 (invoke semanal) com lista Descrição / Urgência / Data
- [ ] Alarme `feedbacks-api-target-5xx` visível no CloudWatch (ação SNS)
- [ ] CI verde (`test(api)` / `deploy(all)` — inclui `harden-rds-sg`) ou `cdk deploy` recente
- [ ] Escalas mínimas (ECS desired=1; RDS micro)

## Cenas

| # | Cena | O que mostrar | Evidência |
|---|------|---------------|-----------|
| 1 | Abertura (30–45s) | Problema → solução; slide da stack (Quarkus + AWS) | Contexto |
| 2 | Arquitetura (1–2 min) | Diagrama API → SQS/Lambda alerta; EventBridge → report → S3/SES; **por que** container + serverless; região demo `us-east-1`; SG RDS endurecido (`harden-rds-sg`) | Cloud + modelo |
| 3 | Health | Postman `GET /api/v1/health` → `{"status":"UP","database":"UP"}`; opcional `/q/health/ready` | FR-13 |
| 4 | Auth | Login Admin + Estudante; tokens JWT | FR-1, FR-2 |
| 5 | Catálogo | Admin cria Curso + Aula; listagens | FR-3, FR-4 |
| 6 | Inscrição + Avaliação OK | Estudante inscreve e `POST /avaliacao` (nota alta/média; body com `aulaId`) | FR-5–FR-7 |
| 7 | Avaliação crítica | `POST /avaliacao` nota ≤4; resposta API com urgência | FR-7, FR-9 |
| 8 | Alerta | Caixa do Admin (SES); assunto de urgência ALTA | FR-10 |
| 9 | Relatórios | Invoke diário/semanal — HTML/PDF com média, qty/dia, qty/urgência e **lista** Descrição/Urgência/Data; ver [UJ4-ROTEIRO](../feedbacks/apps/report/UJ4-ROTEIRO.md) | FR-11, FR-12, FR-17 |
| 10 | PDF no S3 | Objeto `relatorios/.../*.pdf` no console (inclui lista) | FR-12 |
| 11 | Métricas | CloudWatch ALB: RequestCount, 5XX, TargetResponseTime; alarme `feedbacks-api-target-5xx` → SNS (subscription confirmada) | FR-14 |
| 12 | Deploy | Actions `test(api)` / `deploy(all)` verde (pack Lambdas + CDK + `harden-rds-sg`) **ou** `cdk deploy` + task RUNNING | FR-15 |
| 13 | Encerramento | Custo: Actions `pause(demo)` ou `destroy(all)`; link do repo | SM-3, SM-4 |

## Comandos úteis

```bash
# Health via ALB (output ApiHealthUrl / ApiAlbDns)
curl -s "http://<alb-dns>/api/v1/health"

# Relatório (AD-10)
aws lambda invoke --function-name feedbacks-lambda-report \
  --cli-binary-format raw-in-base64-out \
  --payload '{"periodo":"semanal"}' /tmp/report-semanal.json

# Pausar custo após gravação (preferir Actions → pause(demo))
feedbacks/infra/scripts/demo-lifecycle.sh pause
```

## Referências

- Infra: [`feedbacks/infra/README.md`](../feedbacks/infra/README.md)
- UJ-4 detalhado: [`feedbacks/apps/report/UJ4-ROTEIRO.md`](../feedbacks/apps/report/UJ4-ROTEIRO.md)
- PRD §11: `_bmad-output/planning-artifacts/prds/prd-adjt-fase4-2026-07-20/prd.md`
