# Design — Implementar Módulo 05: Alerta SES (Lambda Quarkus)

> O design técnico completo vive em `openspec/modules/05-alerta-ses/design.md`.
> **Este documento não o duplica** — registra decisões de execução desta change.

## Context

- Módulos 01–04 em `develop`: auth, catálogo, inscrição, Avaliação real + Urgência +
  publish pós-commit ALTA via `EvaluationEventPublisher` (contrato AD-5).
- **Estado após apply (esta change):** `feedbacks/apps/notification` (Quarkus Lambda SQS→SES),
  `feedbacks/infra` (CDK **Java**), adapter SQS da API config-driven
  (`feedbacks.messaging.sqs.queue-url` / `FEEDBACKS_SQS_ALERT_QUEUE_URL`); Kafka `%local` e
  Fake `%test` inalterados; SQS sem URL continua no-op seguro.
- Branch: `feature/openspec-05-alerta-ses-lambda` a partir de `develop`.
- Docs do módulo em `openspec/modules/05-alerta-ses/`.

## Goals / Non-Goals

**Goals:**

- Entregar `lambda-notification` (Quarkus + `quarkus-amazon-lambda`) SQS → SES (FR-10).
- E-mail com descrição, urgência e data (`ocorridoEm`); destinatário `adminEmail`.
- Completar adapter SQS da API (config-driven); CDK mínimo do caminho alerta.
- JaCoCo ≥ 90% no app notification; UJ-3 demonstrável (SES sandbox ou evidência log).

**Non-Goals:**

- `lambda-report`, EventBridge, S3 PDF (FR-11+).
- Deploy completo ECS/ECR/CI da API (FR-15 restante).
- Alterar HTTP de Avaliação, auth, Flyway, classificação de Urgência.

## Decisions

### D1 — App sibling `feedbacks/apps/notification`

Novo módulo Maven Quarkus (não embutir Lambda no JAR da API). Packaging `function.zip`
padrão Quarkus (AD-13). Pacote `com.fiap.feedbacks.lambda.notification`.

Alternativa rejeitada: handler “plain SDK” sem Quarkus — diverge do AD-13.

### D2 — Handler SQS event → porta `EmailSender`

Fluxo: deserializar body AD-5 → validar campos obrigatórios → montar subject/body →
`EmailSender.send(to, subject, body)`. Adapter SES (AWS SDK v2); fake em testes.

Lambda **sem** datasource / VPC (AD-5, AD-17).

### D3 — Contrato AD-5 espelhado (sem lib compartilhada no MVP)

Record/DTO na Lambda com os mesmos campos JSON da API (`avaliacaoId`, `descricao`,
`urgencia`, `ocorridoEm`, `aulaId`, `cursoId`). Fixture JSON de teste idêntica à usada
nos testes do publisher da API. Extrair `libs/messaging-contract` fica para depois se
duplicação doer.

### D4 — `adminEmail` via env / Secrets Manager

Env `ADMIN_EMAIL` (ou resolução do secret `adminEmail` no boot). CDK passa o mesmo
secret key (AD-12). Sem e-mail hardcoded.

### D5 — Adapter SQS da API config-driven

Se `feedbacks.messaging.sqs.queue-url` ausente → log no-op (comportamento atual seguro
no CI). Se presente → `SendMessage` com body JSON AD-5. Kafka `%local` e Fake `%test`
inalterados.

### D6 — CDK mínimo só do caminho alerta (Java)

Stack Maven/`software.amazon.awscdk` em `feedbacks/infra`: SQS + DLQ + event source
mapping → Lambda; IAM `ses:SendEmail`; grant read secret `adminEmail`. Sem
ECS/RDS/EventBridge report nesta change. Sem Node/TypeScript no IaC.

### D7 — Conteúdo do e-mail

Subject: prefixo estável (ex. `[Feedbacks] Alerta ALTA`). Body texto (MVP): descrição,
urgência (`ALTA`), data (`ocorridoEm` ISO-8601). HTML opcional — não exigido pelo FR-10.

## Risks / Trade-offs

| Risco | Mitigação |
| --- | --- |
| SES sandbox rejeita destinatário | Doc de verificação; falha → retry/DLQ; demo com identidade verificada |
| CI sem AWS | Unitários + fake SES; CDK `synth` opcional no CI depois (FR-15) |
| Duplicação do DTO AD-5 | Fixture JSON única + campos canônicos na spine |
| Escopo CDK expandir | Checklist explícito: só alerta; report/ECS fora |

## Migration Plan

1. Merge da feature em `develop` com app + testes verdes.
2. `cdk deploy` do stack de alerta (ambiente demo) quando credenciais/SES OK.
3. Configurar queue URL no profile `%aws` da API.
4. Evidência UJ-3: POST Avaliação nota ≤4 → e-mail (ou log SES + mensagem na fila).

Rollback: desabilitar event source / desired concurrency 0; Avaliação e API seguem
operando (AD-4).

## Open Questions

- Disparo local end-to-end (LocalStack vs só unitário + AWS demo): default = unitário +
  AWS sandbox na gravação; LocalStack só se sobrar tempo.
