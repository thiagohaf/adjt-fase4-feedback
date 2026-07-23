# Infra CDK (Java) — caminho de alerta (FR-10)

Stack mínimo: **SQS + DLQ → Lambda Quarkus (`function.zip`) → SES**, com leitura do secret
`adminEmail` (AD-12). **Não** inclui ECS, RDS, `lambda-report` nem EventBridge.

IaC 100% **Java 17 + AWS CDK v2 (Maven)** — sem código TypeScript/JavaScript no repositório.

## Pré-requisitos

1. Build da Lambda: `cd ../apps/notification && mvn -q package` → `target/function.zip`
2. Secret no Secrets Manager (JSON), ex.: `{"adminEmail":"voce@exemplo.com"}` — nome
   padrão `feedbacks/adminEmail`
3. Identidade SES verificada (sandbox) para o e-mail do Administrador
4. JDK 17+, Maven e AWS credentials

## Synth (só Maven)

```bash
cd feedbacks/infra
mvn -q compile exec:java
# gera cdk.out/FeedbacksAlertaNotificationStack.template.json
```

## Deploy

Com CDK CLI (se disponível):

```bash
cdk deploy FeedbacksAlertaNotificationStack
```

Ou publique o template gerado via AWS CLI / Console (`cdk.out/*.template.json`).

Configure a API (`%aws`):

```text
FEEDBACKS_SQS_ALERT_QUEUE_URL=<AlertQueueUrl do output>
```

## Fora de escopo

Deploy completo da API (ECR/ECS/CI) = FR-15.
