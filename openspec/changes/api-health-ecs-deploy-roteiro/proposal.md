# Proposal — FR-13/14/15/16: Health, ECS mínimo, métricas e roteiro

> Spine: AD-1, AD-9, AD-10, AD-11, AD-12, AD-14, AD-17. Módulos 01–06 e stacks de
> alerta/relatório permanecem; esta change **fecha** observabilidade + deploy da API
> e o roteiro de vídeo (prazo SM-2: 28/07/2026).

## Why

UJ-3/UJ-4 e Lambdas estão na AWS, mas a **API ainda não** está em ECS: health não
checa DB (FR-13/AD-11), não há métricas/alarme filmáveis da API (FR-14), o pipeline
só faz `mvn verify` (FR-15 parcial) e falta um roteiro único de gravação (FR-16).
Sem isso as cenas 3, 11–13 do PRD §11 e SM-2 ficam incompletos.

## What Changes

- **FR-13:** SmallRye Health com readiness de DB; `/api/v1/health` e `/q/health`
  refletem falha de dependência crítica (AD-11).
- **FR-15 + FR-14:** CDK `ApiStack` — ECR + ECS Fargate (0.25 vCPU) + ALB
  internet-facing + ≥1 alarme CloudWatch (5XX); GHA publica imagem e (opcional)
  dispara deploy; secrets `jwtSecret` + DB + URL da fila SQS (AD-1, AD-12, AD-17
  exceção demo: RDS público, sem NAT).
- **FR-16:** `docs/ROTEIRO-DEMO.md` consolidando cenas 1–13 (health, fluxo, alerta,
  relatório, PDF, métricas, CI/deploy, teardown).
- Packaging JVM container da API (Dockerfile Quarkus ou equivalente); sem
  reescrever domínio HTTP.

## Capabilities

### New Capabilities

- `api-health-db`: health público e SmallRye com checagem básica de conectividade
  ao PostgreSQL; status DOWN quando o DB está inacessível (FR-13, AD-11).
- `api-ecs-deploy-observability`: API Quarkus em ECS Fargate atrás de ALB; imagem
  no ECR via pipeline; métricas ALB (RequestCount, 4XX/5XX, TargetResponseTime) +
  ≥1 alarme documentado (FR-14, FR-15; AD-1, AD-11, AD-12, AD-17 demo).
- `demo-roteiro`: roteiro único de demonstração YouTube com ordem de cenas,
  comandos/Postman e o que a câmera mostra (FR-16, PRD §11).

### Modified Capabilities

- _(nenhuma)_ — specs HTTP de auth/catálogo/inscrição/avaliação e Lambdas
  permanecem; esta change **não** altera contratos de domínio.

## Impact

- **Código API:** health (SmallRye DB check + `/api/v1/health`); Dockerfile /
  build de imagem; profile `%aws` já existente.
- **Infra:** `feedbacks/infra/` — nova `ApiStack` (ECR, ECS, ALB, SG, alarmes);
  entrypoint CDK atualizado; README + teardown.
- **CI:** estender GHA (build imagem → push ECR; deploy CDK opcional /
  `workflow_dispatch`).
- **Docs:** `docs/ROTEIRO-DEMO.md`; atualizar `feedbacks/infra/README.md`.
- **Branch:** `feature/openspec-api-health-ecs-deploy` a partir de `develop`.
- **Fora de escopo:** VPC/NAT completo (alvo AD-17); multi-AZ; SES fora de sandbox;
  portal web; alterar Lambdas de alerta/relatório.
