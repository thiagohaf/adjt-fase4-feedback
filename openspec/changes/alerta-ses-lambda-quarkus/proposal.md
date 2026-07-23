# Proposal — Implementar Módulo 05: Alerta SES (Lambda Quarkus)

> **Fonte de verdade funcional:** `openspec/modules/05-alerta-ses/` (proposal.md, specs.md, design.md).
> Esta change **deriva** desses documentos — não os reinventa. Publish ALTA (módulo 04 /
> `avaliacao-evento-alerta`) permanece: porta `EvaluationEventPublisher`, contrato AD-5,
> falha não reverte Avaliação. Spine: AD-1, AD-4, AD-5, AD-7, AD-12, AD-13, AD-14, AD-17, AD-18.

## Why

Com Avaliação real e publish pós-commit ALTA entregues, o Administrador ainda **não recebe**
e-mail: não existe `lambda-notification` consumindo o evento AD-5 nem envio SES (FR-10 / UJ-3).
Sem a Lambda SRP, o enunciado de notificação imediata e a cena 8 do roteiro ficam incompletos.
É o próximo módulo de valor após o archive do módulo 04.

## What Changes

- Cria o app Quarkus `feedbacks/apps/notification` (`lambda-notification`) com
  `quarkus-amazon-lambda`: consome mensagem SQS (corpo JSON AD-5) e envia e-mail SES ao
  Administrador (descrição, urgência, data) — **sem** consulta ao RDS (AD-5, AD-13, AD-17).
- Destinatário = `adminEmail` (Secrets Manager / env); sandbox SES OK (endereço verificado).
- Packaging `function.zip`; JaCoCo ≥ 90% no módulo (AD-14).
- Completa o adapter SQS da API (`SqsEvaluationEventPublisher` deixa de ser no-op quando a
  fila estiver configurada) sem alterar o contrato AD-5 nem o comportamento de não-rollback.
- Scaffold CDK mínimo do caminho de alerta (fila SQS + DLQ + Lambda + permissões SES /
  secret) em `feedbacks/infra` — deploy completo ECS/ECR/CI fica no FR-15.
- Testes unitários do parse AD-5 + envio (porta SES fake); fixture JSON compartilhada com o
  contrato da API.
- Atualiza Postman/roteiro com nota de evidência de alerta (log/SES) para UJ-3.

## Capabilities

### New Capabilities

- `alerta-notificacao-ses`: Lambda SRP consome evento AD-5 (SQS) e envia Alerta de urgência
  via SES ao e-mail do Administrador com descrição, urgência e data; MÉDIA/BAIXA não chegam
  à fila (filtro já no publish); falha SES → retry/DLQ sem afetar Avaliação (FR-10; AD-4/5/18).

### Modified Capabilities

- _(nenhuma)_ — requisitos de `avaliacao-evento-alerta` (publish pós-commit, contrato AD-5,
  não-rollback) permanecem; esta change **consome** o evento e **implementa** o adapter SQS
  já previsto, sem mudar requirements de publish.

## Impact

- **Código novo:** `feedbacks/apps/notification/` — handler Quarkus Lambda, parse AD-5,
  porta `EmailSender` + adapter SES, testes + JaCoCo.
- **Código existente:** `SqsEvaluationEventPublisher` (API) — de no-op para publish real
  quando `feedbacks.sqs.queue-url` (ou equivalente) estiver definido; Kafka `%local` / Fake
  test inalterados no comportamento de domínio.
- **Infra:** `feedbacks/infra/` (CDK) — SQS+DLQ+Lambda notification + IAM SES + binding
  `adminEmail`; **fora:** ECS/ALB/RDS full stack, `lambda-report`, EventBridge (FR-11+).
- **Não tocar:** auth, catálogo, inscrição, domínio de Avaliação (FR-7–9), Flyway, paths HTTP.
- **Docs:** collection Postman / nota de demo UJ-3; módulo `openspec/modules/05-alerta-ses/`.
- **Branch:** `feature/openspec-05-alerta-ses-lambda` a partir de `develop`; PR para `develop`.
- **Fora de escopo:** relatórios (FR-11/12/17); health DB/métricas (FR-13/14); pipeline ECR/
  deploy completo da API (FR-15 restante); produção SES fora de sandbox.
