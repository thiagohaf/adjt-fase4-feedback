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

## 3. Verificar e-mail HTML

Caixa do `adminEmail` (SES sandbox): assunto `[Feedbacks] Relatório diario|semanal`,
corpo HTML com tipo, período SP e agregados (zeros se janela vazia — SPEC-12.5).

## 4. Verificar PDF no S3

```bash
aws s3 ls s3://<RelatoriosBucketName>/relatorios/diario/
aws s3 ls s3://<RelatoriosBucketName>/relatorios/semanal/
# chave: relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf
```

## 5. Crons

EventBridge rules `feedbacks-report-diario` / `feedbacks-report-semanal` —
08:00 `America/Sao_Paulo` (= 11:00 UTC). Mesmo algoritmo de janela do invoke.

## Postman

Não há endpoint HTTP de relatório (AD-10). Coleção Postman da API permanece
inalterada; evidência UJ-4 = invoke CLI/console + e-mail + objeto S3.
