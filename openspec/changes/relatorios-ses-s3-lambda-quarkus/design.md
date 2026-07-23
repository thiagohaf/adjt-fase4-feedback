# Design — Implementar Módulo 06: Relatórios periódicos (Lambda Quarkus)

> O design técnico completo vive em `openspec/modules/06-relatorios/design.md`.
> **Este documento não o duplica** — registra decisões de execução desta change.

## Context

- Módulos 01–05 em `develop`: auth, catálogo, inscrição, Avaliação + Urgência, alerta
  SES (`feedbacks/apps/notification` + `AlertaNotificationStack`).
- **Estado após apply (esta change):** `feedbacks/apps/report` (Quarkus Lambda
  EventBridge/invoke → agrega RDS → SES HTML + PDF S3), CDK estendido com stack de
  relatório (EventBridge + Lambda VPC + S3); `lambda-notification` inalterada (SRP).
- Branch: `feature/openspec-06-relatorios-lambda` a partir de `develop`.
- Docs do módulo em `openspec/modules/06-relatorios/`.

## Goals / Non-Goals

**Goals:**

- Entregar `lambda-report` (Quarkus + `quarkus-amazon-lambda`) com `periodo=diario|semanal`
  (FR-11, FR-17, AD-6, AD-13).
- Agregar Avaliações read-only no RDS (`nota`, `urgencia`, `ocorrido_em`) — AD-3, AD-16.
- E-mail HTML ao `adminEmail` + PDF tabular no S3
  (`relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf`) — FR-12.
- Crons EventBridge 08:00 `America/Sao_Paulo` (diário; segunda semanal) + invoke manual
  com o **mesmo** algoritmo de janela (AD-10).
- JaCoCo ≥ 90% no app report; UJ-4 demonstrável.

**Non-Goals:**

- Endpoint HTTP admin de relatório (AD-10).
- Alterar `lambda-notification`, publish AD-5, HTTP de Avaliação.
- Deploy completo ECS/ECR/CI da API (FR-15).
- Health DB / métricas CloudWatch API (FR-13/14).
- Relatórios ad-hoc além de diário/semanal.

## Decisions

### D1 — App sibling `feedbacks/apps/report`

Novo módulo Maven Quarkus (espelho de `notification`). Packaging `function.zip`
(AD-13). Pacote `com.fiap.feedbacks.lambda.report`.

Alternativa rejeitada: embutir relatório em `notification` — viola SRP e AD-1/AD-6.

### D2 — Uma Lambda, parâmetro `periodo`

Input JSON: `{ "periodo": "diario" | "semanal" }`. EventBridge rules usam Input
transformer/constante; invoke manual (CLI/console) envia o mesmo JSON (AD-6, AD-10).

Alternativa rejeitada: duas Lambdas 1:1 com cron — AD-6 proíbe.

### D3 — Janelas civis `America/Sao_Paulo` (âncora `ocorrido_em`)

Dado `triggerInstant` (default = agora):

| periodo | Janela `[inicio, fim)` |
| --- | --- |
| `diario` | dia civil **anterior** a `triggerInstant` em SP |
| `semanal` | **7** dias civis anteriores ao dia civil de `triggerInstant` em SP |

Filtro SQL/ORM **somente** em `ocorrido_em` (timestamptz). Manual invoke usa o mesmo
algoritmo (não “últimas 24h rolling”).

### D4 — Agregados AD-16

- **Diário:** média de `nota`; qty total; qty por `urgencia` (`ALTA`/`MEDIA`/`BAIXA`).
- **Semanal:** média de `nota`; qty **por dia civil SP**; qty por urgência.

Urgência = valor **persistido** (não recalcular a partir da nota). Período sem
avaliações: relatório com zeros / listas vazias — ainda envia e-mail e PDF (demo
previsível).

### D5 — Read model sem Flyway na Lambda

Lambda **read-only** via JDBC/ORM leve ou SQL nativo na tabela `avaliacao` (ou view
criada **só** se a API já tiver migration — preferir tabela existente). Sem migrations
no módulo report (AD-3, AD-16). Em testes: porta `AvaliacaoReadModel` com fake
in-memory.

Alternativa rejeitada: Lambda com Flyway próprio.

### D6 — PDF tabular + HTML SES

- PDF: biblioteca leve (ex. OpenPDF/PDFBox) — tabela com métricas do período.
- E-mail: HTML identificando tipo (`diario`/`semanal`) + período + agregados.
- Portas: `ReportEmailSender`, `ReportPdfStore` (S3); fakes em teste.
- Destinatário: `ADMIN_EMAIL` / secret `adminEmail` (mesmo binding do alerta, AD-12).

### D7 — CDK: stack de relatório (Java)

Estender `feedbacks/infra` com `RelatorioStack` (ou equivalente):

- Bucket S3 de relatórios.
- Lambda a partir de `apps/report` `function.zip`, **na VPC** com SG → RDS:5432
  (AD-17); se VPC/RDS ainda não existirem no repo, stack aceita props/IDs importados
  **ou** documenta dependência de recursos demo — sem inventar ECS completo.
- EventBridge Scheduler/Rule: cron diário 08:00 SP + segunda 08:00 SP → Lambda com
  `periodo` fixo.
- IAM: `ses:SendEmail`, `s3:PutObject`, read secrets `adminEmail` + DB.
- **Não** alterar comportamento do `AlertaNotificationStack` além do mínimo necessário
  para compartilhar secrets/VPC se o design CDK exigir.

### D8 — Sem rota HTTP de relatório

Demo = `aws lambda invoke` / console com payload `periodo` (AD-10).

## Risks / Trade-offs

| Risco | Mitigação |
| --- | --- |
| VPC/RDS ainda não provisionados no CDK atual | Stack parametrizada + unitários com fake read model; deploy AWS só quando DB demo existir |
| Cold start Quarkus + JDBC na VPC | Sizing demo; timeout generoso; PDF simples |
| SES sandbox | Mesmo `adminEmail` verificado do módulo 05 |
| Divergência janela TZ | Testes de fronteira (meia-noite SP, DST se aplicável) documentados |
| Escopo CDK virar FR-15 | Checklist: sem ECR/ECS pipeline; só report path |

## Migration Plan

1. Merge feature em `develop` com app + testes verdes.
2. `mvn package` → `function.zip`; `cdk deploy` do stack de relatório (quando VPC/RDS
   demo OK).
3. Evidência UJ-4: invoke `periodo=diario` e/ou `semanal` → e-mail HTML + objeto no S3.
4. Opcional: deixar crons habilitados só na janela de gravação (teardown AD-12).

Rollback: desabilitar regras EventBridge / concurrency 0; API e alerta seguem (AD-4).

## Open Questions

- Provisionar RDS mínimo neste módulo vs. importar VPC/DB já existentes na conta demo:
  default = **props importáveis + fake em CI**; RDS greenfield só se bloqueador de demo.
- Biblioteca PDF final (OpenPDF vs PDFBox): escolher na implementação pela menor
  superfície Maven.
