# Tasks — Implementar Módulo 06: Relatórios periódicos (Lambda Quarkus)

> Referências: `specs/relatorio-periodico-ses-s3/spec.md`, `design.md` (D1–D8) e
> `openspec/modules/06-relatorios/{proposal,design,specs}.md`.

## 1. Branch e baseline

- [x] 1.1 Criar branch `feature/openspec-06-relatorios-lambda` a partir de `develop`
- [x] 1.2 Confirmar ausência de `apps/report`, presença de `apps/notification` +
      `AlertaNotificationStack`, e schema Avaliação com `nota`/`urgencia`/`ocorrido_em`

## 2. Scaffold do app report

- [x] 2.1 Criar `feedbacks/apps/report` (Maven Quarkus 3.33, Java 17,
      `quarkus-amazon-lambda`, Jackson, JDBC/ORM read-only, JaCoCo gate ≥ 90%)
- [x] 2.2 Configurar packaging `function.zip` e handler Quarkus (AD-13); pacote
      `com.fiap.feedbacks.lambda.report`

## 3. Domínio: período, janela e agregados

- [x] 3.1 Implementar `Periodo` (`diario`|`semanal`) + `ReportWindow` com algoritmo
      civil `America/Sao_Paulo` (SPEC-11.2, SPEC-17.1; D3)
- [x] 3.2 Implementar `ReportAggregates` (média, qty total, qty/dia, qty/urgência)
      conforme AD-16 / SPEC-11.1 / SPEC-17.1
- [x] 3.3 Testes unitários de fronteira de janela (meia-noite SP) e período vazio

## 4. Application e handler

- [x] 4.1 Portas `AvaliacaoReadModel`, `ReportEmailSender`, `ReportPdfStore` +
      `GenerateReportUseCase` (janela → agrega → PDF → SES)
- [x] 4.2 Handler Lambda: parse `{ "periodo": ... }`; `periodo` inválido falha;
      período vazio ainda entrega (SPEC-12.5)
- [x] 4.3 Garantir ausência de writes de domínio (SPEC-11.3)

## 5. Adapters: read model, SES HTML, S3 PDF

- [x] 5.1 Adapter read-only JDBC/ORM na tabela Avaliação (campos AD-16); Fake
      in-memory para testes
- [x] 5.2 `SesReportEmailSender` HTML (tipo + período + agregados) + Fake; destinatário
      `ADMIN_EMAIL` / `adminEmail` (SPEC-12.1, SPEC-12.4)
- [x] 5.3 Gerador PDF tabular + `S3ReportPdfStore` com key
      `relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf` (SPEC-12.2); Fake store em testes

## 6. CDK (caminho relatório)

- [x] 6.1 Criar `RelatorioStack` (ou equivalente) em `feedbacks/infra`: bucket S3 +
      Lambda a partir de `function.zip` do report
- [x] 6.2 EventBridge: cron diário 08:00 SP + segunda 08:00 SP com `periodo` fixo;
      documentar invoke manual (SPEC-12.3, AD-10)
- [x] 6.3 IAM SES/S3 + secrets `adminEmail` e DB; Lambda na VPC com SG → RDS quando
      props disponíveis (AD-17); sem ECS/ECR pipeline (FR-15)

## 7. Testes, docs e critério de saída

- [x] 7.1 Unitários SPEC-11.* / SPEC-17.* / SPEC-12.*; `mvn verify` no módulo report
      com JaCoCo ≥ 90%
- [x] 7.2 Nota Postman/roteiro UJ-4 (invoke + e-mail + PDF S3); checkboxes proposal
      módulo 06 §6
- [x] 7.3 Confirmar auth/catálogo/inscrição/Avaliação HTTP e `notification` inalterados
