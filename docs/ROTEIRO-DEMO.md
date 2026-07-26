# Roteiro de demonstração (YouTube) — FR-16 / PRD §11

Tempo-alvo: **8–15 minutos**. Ordem sugerida para não se perder na gravação.

**Pré-requisitos AWS (demo):** RDS `feedbacks-demo` available; EventBridge
`feedbacks-report-*` ENABLED; stack `FeedbacksApiStack` deployed (ALB);
SES sandbox com e-mail verificado; secret `feedbacks/jwt` + `feedbacks/db`.

ALB demo (atualizar se redeploy — output `ApiAlbDns`):  
`http://Feedba-ApiSe-hGCfix1THZyT-1147675187.us-east-1.elb.amazonaws.com`  
Health: `/api/v1/health` · Ready: `/q/health/ready` · Alarme: `feedbacks-api-target-5xx`

## Checklist pré-gravação

- [ ] SES: identidade verificada; checar **Spam** no Gmail
- [ ] Seeds de usuários (Flyway) + seed relatório (`seed-report-demo.sql`) se UJ-4
- [ ] Postman: `docs/postman/feedbacks-api.postman_collection.json`
- [ ] Um PDF de relatório no S3 (invoke semanal)
- [ ] Alarme `feedbacks-api-target-5xx` visível no CloudWatch
- [ ] CI verde (`ci(api)` e/ou `deploy(api)`) ou `cdk deploy` recente
- [ ] Escalas mínimas (ECS desired=1; RDS micro)

## Cenas

| # | Cena | O que mostrar | Evidência |
|---|------|---------------|-----------|
| 1 | Abertura (30–45s) | Problema → solução; slide da stack (Quarkus + AWS) | Contexto |
| 2 | Arquitetura (1–2 min) | Diagrama API → SQS/Lambda alerta; EventBridge → report → S3/SES; **por que** container + serverless; região demo `us-east-1` | Cloud + modelo |
| 3 | Health | Postman `GET /api/v1/health` → `{"status":"UP","database":"UP"}`; opcional `/q/health/ready` | FR-13 |
| 4 | Auth | Login Admin + Estudante; tokens JWT | FR-1, FR-2 |
| 5 | Catálogo | Admin cria Curso + Aula; listagens | FR-3, FR-4 |
| 6 | Inscrição + Avaliação OK | Estudante inscreve e avalia nota alta/média | FR-5–FR-7 |
| 7 | Avaliação crítica | Nota ≤4; resposta API com urgência | FR-7, FR-9 |
| 8 | Alerta | Caixa do Admin (SES); assunto de urgência ALTA | FR-10 |
| 9 | Relatórios | Invoke diário/semanal (CLI/console) — ver [UJ4-ROTEIRO](../feedbacks/apps/report/UJ4-ROTEIRO.md) | FR-11, FR-12, FR-17 |
| 10 | PDF no S3 | Objeto `relatorios/.../*.pdf` no console | FR-12 |
| 11 | Métricas | CloudWatch ALB: RequestCount, 5XX, TargetResponseTime; alarme `feedbacks-api-target-5xx` | FR-14 |
| 12 | Deploy | Actions `ci(api)` / `deploy(api)` verde **ou** `cdk deploy FeedbacksApiStack` + task RUNNING | FR-15 |
| 13 | Encerramento | Custo, teardown (ECS desired=0 / destroy, RDS stop, crons DISABLED), link do repo | SM-3, SM-4 |

## Comandos úteis

```bash
# Health via ALB (output ApiHealthUrl / ApiAlbDns)
curl -s "http://<alb-dns>/api/v1/health"

# Relatório (AD-10)
aws lambda invoke --function-name feedbacks-lambda-report \
  --cli-binary-format raw-in-base64-out \
  --payload '{"periodo":"semanal"}' /tmp/report-semanal.json

# Pausar custo após gravação
aws ecs update-service --cluster feedbacks-api --service feedbacks-api --desired-count 0
aws rds stop-db-instance --db-instance-identifier feedbacks-demo
aws events disable-rule --name feedbacks-report-diario
aws events disable-rule --name feedbacks-report-semanal
```

## Referências

- Infra: [`feedbacks/infra/README.md`](../feedbacks/infra/README.md)
- UJ-4 detalhado: [`feedbacks/apps/report/UJ4-ROTEIRO.md`](../feedbacks/apps/report/UJ4-ROTEIRO.md)
- PRD §11: `_bmad-output/planning-artifacts/prds/prd-adjt-fase4-2026-07-20/prd.md`
