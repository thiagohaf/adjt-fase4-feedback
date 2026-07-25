# UJ-4 — Relatório periódico (invoke + SES + S3)

Roteiro de evidência após `cdk deploy FeedbacksRelatorioStack`.

## 1. Build

```bash
cd feedbacks/apps/report && mvn -q package
# artefato: target/function.zip
```

## 2. Invoke manual (AD-10)

```bash
aws lambda invoke \
  --function-name feedbacks-lambda-report \
  --cli-binary-format raw-in-base64-out \
  --payload '{"periodo":"diario"}' \
  /tmp/report-diario.json && cat /tmp/report-diario.json

aws lambda invoke \
  --function-name feedbacks-lambda-report \
  --cli-binary-format raw-in-base64-out \
  --payload '{"periodo":"semanal"}' \
  /tmp/report-semanal.json && cat /tmp/report-semanal.json
```

`periodo` inválido deve falhar (não entrega e-mail/PDF).

Resposta esperada com RDS + seed: `"total"` > 0 (ex. semanal com 8 avaliações
nos últimos 7 dias). Sem JDBC: `total=0` (SPEC-12.5).

## 3. Verificar e-mail HTML

Caixa do `adminEmail` (SES sandbox; frequentemente **Spam**): assunto
`[Feedbacks] Relatório diario|semanal`, corpo HTML com tipo, período SP e agregados.

## 4. Verificar PDF no S3

```bash
aws s3 ls s3://<RelatoriosBucketName>/relatorios/diario/
aws s3 ls s3://<RelatoriosBucketName>/relatorios/semanal/
# chave: relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf
```

## 5. Crons

EventBridge rules `feedbacks-report-diario` / `feedbacks-report-semanal` —
08:00 `America/Sao_Paulo` (= 11:00 UTC). Mesmo algoritmo de janela do invoke.

## 6. Opcional — dados reais no RDS (demo)

1. RDS Postgres público + secret `feedbacks/db` (ver `feedbacks/infra/README.md`).
2. Flyway V1–V5 da API + [`scripts/seed-report-demo.sql`](../../infra/scripts/seed-report-demo.sql).
3. Redeploy: `FEEDBACKS_DB_SECRET_NAME=feedbacks/db` (Lambda fora de VPC).
4. Invoke `semanal` → logs `Read model window ... → N linhas` com N > 0;
   e-mail/PDF com média e qty por urgência ≠ zero.
5. **Teardown** RDS + secret após gravação.

## Postman

Não há endpoint HTTP de relatório (AD-10). Coleção Postman da API permanece
inalterada; evidência UJ-4 = invoke CLI/console + e-mail + objeto S3.
