# Infra CDK (Java) — alerta (FR-10) + relatórios (FR-11/12/17)

IaC 100% **Java 17 + AWS CDK v2 (Maven)** — sem TypeScript/JavaScript no repositório.

## Stacks

| Stack | Conteúdo |
| --- | --- |
| `FeedbacksAlertaNotificationStack` | SQS + DLQ → `lambda-notification` → SES |
| `FeedbacksRelatorioStack` | EventBridge (diário/semanal) → `lambda-report` → SES HTML + PDF S3 |

**Fora de escopo:** ECS/ECR/CI completo da API (FR-15).

## Pré-requisitos

### Alerta
1. `cd ../apps/notification && mvn -q package` → `target/function.zip`
2. Secret `feedbacks/adminEmail` JSON: `{"adminEmail":"voce@exemplo.com"}`
3. Identidade SES verificada (sandbox)

### Relatório
1. `cd ../apps/report && mvn -q package` → `target/function.zip`
2. Mesmo secret `adminEmail` (e SES sandbox)
3. **Dados reais (demo acadêmico):** secret `feedbacks/db` JSON
   `{"dbUrl":"jdbc:postgresql://...","dbUser":"...","dbPassword":"..."}`  
   e deploy com `FEEDBACKS_DB_SECRET_NAME=feedbacks/db` (liga JDBC).
4. **Opcional — alvo AD-17:** `FEEDBACKS_REPORT_VPC_ID`, `FEEDBACKS_REPORT_SG_ID`  
   (private + egress). Sem isso a Lambda fica **fora de VPC** (SES/S3 ok).

#### Demo RDS público (exceção AD-17 por custo)

Conta tipicamente sem NAT: provisionar Postgres público (`db.t4g.micro`, SG `:5432`
aberto só para demo), aplicar Flyway V1–V5 da API, seed
[`scripts/seed-report-demo.sql`](scripts/seed-report-demo.sql), criar secret
`feedbacks/db`, redeploy com `FEEDBACKS_DB_SECRET_NAME` **sem** VPC env.

```bash
export CDK_DEFAULT_ACCOUNT=... CDK_DEFAULT_REGION=us-east-1
export FEEDBACKS_DB_SECRET_NAME=feedbacks/db
cdk deploy FeedbacksRelatorioStack
```

**Teardown:** apagar instância RDS `feedbacks-demo`, SG `feedbacks-rds-demo`,
subnet group `feedbacks-demo` e secret `feedbacks/db` após o vídeo.

Sem secret DB → `FEEDBACKS_REPORT_JDBC_ENABLED=false` → agregados zerados (SPEC-12.5).

## Synth

```bash
cd feedbacks/infra
mvn -q compile exec:java
# gera cdk.out/*.template.json
```

## Deploy

```bash
cdk deploy FeedbacksAlertaNotificationStack FeedbacksRelatorioStack
```

### Invoke manual (AD-10 / UJ-4)

```bash
aws lambda invoke \
  --function-name feedbacks-lambda-report \
  --cli-binary-format raw-in-base64-out \
  --payload '{"periodo":"diario"}' \
  /tmp/report-out.json

aws lambda invoke \
  --function-name feedbacks-lambda-report \
  --cli-binary-format raw-in-base64-out \
  --payload '{"periodo":"semanal"}' \
  /tmp/report-out.json
```

Crons EventBridge: **08:00 America/Sao_Paulo** (= `cron(0 11 ...)` UTC, BRT UTC-3) —
diário todos os dias; semanal às segundas — payload fixo `periodo=diario|semanal`.

## API (alerta)

```text
FEEDBACKS_SQS_ALERT_QUEUE_URL=<AlertQueueUrl do output>
```
