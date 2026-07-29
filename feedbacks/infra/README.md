# Infra CDK (Java) — alerta (FR-10) + relatórios (FR-11/12/17) + API ECS (FR-13/14/15)

IaC 100% **Java 17 + AWS CDK v2 (Maven)** — sem TypeScript/JavaScript no repositório.

## Stacks

| Stack | Conteúdo |
| --- | --- |
| `FeedbacksAlertaNotificationStack` | SQS + DLQ → `lambda-notification` → SES |
| `FeedbacksRelatorioStack` | EventBridge (diário/semanal) → `lambda-report` → SES HTML + PDF S3 |
| `FeedbacksApiStack` | ECR + ECS Fargate + ALB + alarme CloudWatch 5XX → SNS |

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
4. A Lambda report entra na **VPC default** (public + `allowPublicSubnet`) com SG
   `feedbacks-report-lambda` (output `ReportLambdaSecurityGroupId`).

#### Demo RDS + SG endurecido (AD-17 demo)

Postgres público (`db.t4g.micro`) para endpoint estável; após `cdk deploy` + seed,
`demo-lifecycle.sh harden-rds-sg` autoriza só **ECS SG** + **Report Lambda SG** e
**remove** `0.0.0.0/0:5432`. HTTPS no ALB se `FEEDBACKS_ACM_CERT_ARN` (secret/env).

```bash
export CDK_DEFAULT_ACCOUNT=... CDK_DEFAULT_REGION=us-east-1
export FEEDBACKS_DB_SECRET_NAME=feedbacks/db
cdk deploy FeedbacksRelatorioStack
# após ApiStack + RelatorioStack:
./scripts/demo-lifecycle.sh harden-rds-sg
```

### API (ECS / FR-13–15)

1. Docker Desktop (ou daemon) para o asset `apps/api/Dockerfile`
2. Secret `feedbacks/jwt` JSON: `{"jwtSecret":"<≥256 bits>"}`
3. Secret `feedbacks/db` (mesmas chaves do relatório)
4. Fila SQS de alerta (output `AlertQueueUrl` ou `FEEDBACKS_SQS_ALERT_QUEUE_URL`)

```bash
export CDK_DEFAULT_ACCOUNT=... CDK_DEFAULT_REGION=us-east-1
export FEEDBACKS_DB_SECRET_NAME=feedbacks/db
export FEEDBACKS_JWT_SECRET_NAME=feedbacks/jwt
export FEEDBACKS_SQS_ALERT_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/.../feedbacks-avaliacao-alerta
cdk deploy FeedbacksApiStack
```

Outputs: `ApiAlbDns`, `ApiHealthUrl`, `ApiAlarmName`, `ApiAlarmTopicArn`,
`ApiEcsSecurityGroupId`, `ApiEcrRepositoryUri`, `ApiHttpsEnabled`.

Health: `GET http(s)://<ApiAlbDns>/api/v1/health` e probe ALB `/q/health/ready`.
Avaliação: `POST /avaliacao`. Confirme a subscription SNS do alarme no e-mail admin.

Pipeline (GitHub Actions → **Run workflow**):

| Workflow | Arquivo | Função |
| --- | --- | --- |
| `test(api)` | `test-api.yml` | `mvn verify` + JaCoCo ≥ 90% |
| **`deploy(all)`** | `deploy-all.yml` | sobe/retoma tudo; **recria RDS+secrets** se destroy apagou |
| **`pause(demo)`** | `pause-demo.yml` | corta custo (crons off, destroy API/ALB, stop RDS) |
| **`destroy(all)`** | `destroy-all.yml` | apaga stacks (+ RDS/secrets opcional); confirme `destroy` |

Script compartilhado: [`scripts/demo-lifecycle.sh`](scripts/demo-lifecycle.sh).

**Pausa local:** `./scripts/demo-lifecycle.sh pause`  
**Destroy local:** `./scripts/demo-lifecycle.sh destroy-all true false`

Sem secret DB no report → `FEEDBACKS_REPORT_JDBC_ENABLED=false` → agregados zerados (SPEC-12.5).

## Synth

```bash
cd feedbacks/infra
mvn -q compile exec:java
# gera cdk.out/*.template.json
```

## Deploy

```bash
cdk deploy FeedbacksAlertaNotificationStack FeedbacksRelatorioStack FeedbacksApiStack
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

Roteiro de vídeo: [`docs/ROTEIRO-DEMO.md`](../../docs/ROTEIRO-DEMO.md).
