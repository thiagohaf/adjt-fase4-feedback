# Proposal — Implementar Módulo 06: Relatórios periódicos (Lambda Quarkus)

> **Fonte de verdade funcional:** `openspec/modules/06-relatorios/` (proposal.md, specs.md, design.md).
> Esta change **deriva** desses documentos — não os reinventa. Spine: AD-1, AD-3, AD-6,
> AD-10, AD-12, AD-13, AD-14, AD-16, AD-17, AD-18. Alerta (módulo 05 / FR-10) permanece
> SRP em `lambda-notification` — **não** misturar com relatório.

## Why

Com alerta SES (FR-10) entregue, o Administrador ainda **não recebe** consolidação
periódica: não existe `lambda-report`, EventBridge, agregação no RDS nem PDF no S3
(FR-11, FR-17, FR-12 / UJ-4). Sem isso o enunciado (≥2 serverless SRP + relatório
semanal) e as cenas 9–10 do roteiro ficam incompletos. É o próximo módulo de valor
após o archive do módulo 05.

## What Changes

- Cria o app Quarkus `feedbacks/apps/report` (`lambda-report`) com
  `quarkus-amazon-lambda`: recebe `periodo=diario|semanal` (EventBridge ou invoke
  manual AD-10), agrega Avaliações no RDS (read-only, AD-3/AD-16), envia e-mail HTML
  via SES e grava PDF tabular no S3 (AD-6, FR-11/12/17).
- Janelas civis `America/Sao_Paulo`: diário = dia civil anterior; semanal = 7 dias
  civis anteriores; filtro por `ocorrido_em` (AD-6, AD-16).
- Packaging `function.zip`; JaCoCo ≥ 90% no módulo (AD-14).
- Estende CDK em `feedbacks/infra`: EventBridge (cron diário/semanal), Lambda report
  **na VPC** com acesso RDS, bucket S3, IAM SES/S3/Secrets (`adminEmail` + DB), sem
  ECS/ECR/CI completo (FR-15).
- Disparo de demo = invoke Lambda com `periodo` — **sem** endpoint HTTP admin (AD-10).
- Testes unitários de janelas/agregação/PDF/e-mail (portas fake); docs Postman/roteiro
  UJ-4.

## Capabilities

### New Capabilities

- `relatorio-periodico-ses-s3`: uma `lambda-report` Quarkus gera Relatório diário e
  semanal (agregados AD-16), envia HTML SES ao Administrador e publica PDF no S3 com
  chave `relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf`; crons EventBridge 08:00 SP;
  invoke manual com o mesmo algoritmo de janela (FR-11, FR-17, FR-12; AD-6/10/16).

### Modified Capabilities

- _(nenhuma)_ — `alerta-notificacao-ses` e specs de Avaliação/auth permanecem; esta
  change **não** altera requisitos de alerta nem HTTP de domínio.

## Impact

- **Código novo:** `feedbacks/apps/report/` — handler Quarkus Lambda, cálculo de
  janela, repositório read-only de agregados, gerador PDF, porta e-mail HTML +
  adapter SES/S3, testes + JaCoCo.
- **Código existente:** API / Flyway — apenas se AD-16 exigir view ou índice de leitura
  documentado; **sem** mutação de domínio nem endpoint de relatório.
- **Infra:** `feedbacks/infra/` (CDK Java) — EventBridge + Lambda report (VPC) + S3 +
  IAM; reutiliza secrets `adminEmail` e DB; **fora:** pipeline ECR/ECS full (FR-15),
  health/métricas API (FR-13/14).
- **Não tocar:** auth, catálogo, inscrição, Avaliação HTTP, `lambda-notification`,
  publish AD-5.
- **Docs:** `openspec/modules/06-relatorios/`; nota Postman/roteiro UJ-4 (e-mail + PDF).
- **Branch:** `feature/openspec-06-relatorios-lambda` a partir de `develop`; PR para
  `develop`.
- **Fora de escopo:** relatórios ad-hoc além de diário/semanal; produção SES fora de
  sandbox; LocalStack obrigatório; FR-13/14/15.
