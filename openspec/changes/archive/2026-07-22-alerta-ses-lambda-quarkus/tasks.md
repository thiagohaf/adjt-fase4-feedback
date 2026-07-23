# Tasks — Implementar Módulo 05: Alerta SES (Lambda Quarkus)

> Referências: `specs/alerta-notificacao-ses/spec.md`, `design.md` (D1–D7) e
> `openspec/modules/05-alerta-ses/{proposal,design,specs}.md`.

## 1. Branch e baseline

- [x] 1.1 Criar branch `feature/openspec-05-alerta-ses-lambda` a partir de `develop`
- [x] 1.2 Confirmar publish ALTA (`EvaluationEventPublisher`, adapters no-op Kafka/SQS/Fake)
      e ausência de `apps/notification` / `infra` — baseline

## 2. Scaffold do app notification

- [x] 2.1 Criar `feedbacks/apps/notification` (Maven Quarkus 3.33, Java 17,
      `quarkus-amazon-lambda`, Jackson, JaCoCo gate ≥ 90%)
- [x] 2.2 Configurar packaging `function.zip` e handler Quarkus (AD-13); pacote
      `com.fiap.feedbacks.lambda.notification`

## 3. Domínio e application da Lambda

- [x] 3.1 Criar `AlertPayload` (campos AD-5) + parse/validação de JSON UTF-8
- [x] 3.2 Criar porta `EmailSender` + `ProcessAlertUseCase` (parse → montar e-mail → send)
- [x] 3.3 Implementar handler SQS que delega ao use case; falha de parse/SES propaga
      para retry/DLQ (SPEC-10.4, SPEC-10.5)

## 4. Adapter SES e configuração

- [x] 4.1 Implementar `SesEmailSender` (AWS SDK v2) + `FakeEmailSender` para testes
- [x] 4.2 Resolver destinatário via `ADMIN_EMAIL` / secret `adminEmail` (SPEC-10.3);
      subject/body com descrição, urgência e `ocorridoEm` (SPEC-10.1)
- [x] 4.3 Garantir ausência de JDBC/ORM no módulo (SPEC-10.2, SPEC-10.6)

## 5. Adapter SQS na API

- [x] 5.1 Evoluir `SqsEvaluationEventPublisher`: publish real quando queue URL configurada;
      manter no-op seguro se ausente
- [x] 5.2 Propriedade `%aws` documentada; regressão Fake/Kafka e testes de publish
      (SPEC-10prep.*) verdes

## 6. CDK mínimo (caminho alerta)

- [x] 6.1 Scaffold `feedbacks/infra` (CDK Java): SQS + DLQ + Lambda + event source
- [x] 6.2 IAM SES + leitura secret `adminEmail` → env da Lambda; sem ECS/RDS/report

## 7. Testes, docs e critério de saída

- [x] 7.1 Unitários SPEC-10.1–10.6 (fixture JSON AD-5 alinhada à API); `mvn verify` no
      módulo notification com JaCoCo ≥ 90%
- [x] 7.2 Nota Postman/roteiro UJ-3 (evidência alerta); checkboxes proposal módulo 05 §6
- [x] 7.3 Confirmar auth/catálogo/inscrição/Avaliação HTTP inalterados
