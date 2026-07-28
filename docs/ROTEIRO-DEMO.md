# Roteiro de demonstração (YouTube) — FR-16 / PRD §11

Tempo-alvo: **8–15 minutos**. Ordem sugerida para não se perder na gravação.

**Pré-requisitos AWS (demo):** RDS `feedbacks-demo` available; EventBridge
`feedbacks-report-*` ENABLED; stack `FeedbacksApiStack` deployed (ALB);
SES sandbox com e-mail verificado; secret `feedbacks/jwt` + `feedbacks/db`.

**Ciclo de vida (Actions → Run workflow):**
- **`deploy(all)`** — sobe/retoma tudo (RDS + 3 stacks + crons + seed + smoke)
- **`pause(demo)`** — corta custo horário (crons off, destroy API/ALB, stop RDS)
- **`destroy(all)`** — apaga stacks (e opcionalmente RDS/secrets); confirme com `destroy`

Atualize o `baseUrl` do Postman com o `ApiAlbDns` do summary do `deploy(all)`.

ALB demo (atualizar se redeploy — output `ApiAlbDns`):  
`http://Feedba-ApiSe-hGCfix1THZyT-1147675187.us-east-1.elb.amazonaws.com`  
Health: `/api/v1/health` · Ready: `/q/health/ready` · Alarme: `feedbacks-api-target-5xx`

## Checklist pré-gravação

- [ ] SES: identidade verificada; checar **Spam** no Gmail
- [ ] Seeds de usuários (Flyway) + seed relatório (`seed-report-demo.sql`) se UJ-4
- [ ] Postman: collection + env AWS ALB em `docs/postman/` (selecionar environment **AWS ALB**)
- [ ] Um PDF de relatório no S3 (invoke semanal)
- [ ] Alarme `feedbacks-api-target-5xx` visível no CloudWatch
- [ ] CI verde (`ci(api)` / `deploy(all)`) ou `cdk deploy` recente
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
| 12 | Deploy | Actions `ci(api)` / `deploy(all)` verde **ou** `cdk deploy` + task RUNNING | FR-15 |
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
