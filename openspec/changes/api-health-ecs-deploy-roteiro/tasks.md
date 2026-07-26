# Tasks — FR-13/14/15/16: Health, ECS, métricas e roteiro

> Specs: `api-health-db`, `api-ecs-deploy-observability`, `demo-roteiro`. Design D1–D7.

## 1. Branch e baseline

- [x] 1.1 Criar branch `feature/openspec-api-health-ecs-deploy` a partir de `develop`
- [x] 1.2 Confirmar secrets DB/SQS + RDS available; JWT secret a criar no deploy (`feedbacks/jwt`)

## 2. FR-13 — Health com DB

- [x] 2.1 Garantir checagem de DB no SmallRye (`/q/health` / `/q/health/ready`)
- [x] 2.2 Fazer `GET /api/v1/health` refletir readiness (UP só com DB ok)
- [x] 2.3 Testes `@QuarkusTest` cobrindo health UP; documentar path de falha DB
- [x] 2.4 `mvn verify` no módulo api com JaCoCo ≥ 90%

## 3. Container da API

- [x] 3.1 Dockerfile multi-stage (Maven → JRE 17) para a API
- [x] 3.2 Validar boot da imagem (amd64) + health via ALB

## 4. FR-15/14 — CDK ApiStack + alarme

- [x] 4.1 Criar `ApiStack` (ECR, Cluster, Task/Service Fargate 0.25/1024, ALB, SG)
- [x] 4.2 Health check ALB → `/q/health/ready`; env/secrets JWT+DB+SQS (D4)
- [x] 4.3 Alarme CloudWatch ≥1 (ex.: target 5XX); outputs ALB DNS + alarm name
- [x] 4.4 Registrar stack no entrypoint CDK; atualizar `infra/README.md` + teardown

## 5. Pipeline GHA

- [x] 5.1 Workflow build/push ECR (+ deploy `workflow_dispatch` ou documentado)
- [x] 5.2 Evidência: `cdk deploy` com serviço RUNNING + health UP

## 6. FR-16 — Roteiro

- [x] 6.1 Escrever `docs/ROTEIRO-DEMO.md` (cenas 1–13 + checklist pré-gravação)
- [x] 6.2 Referenciar Postman, UJ4-ROTEIRO, métricas/alarme, CI

## 7. Critério de saída

- [x] 7.1 Health UP via ALB; métricas/alarme configurados (alarme `feedbacks-api-target-5xx`)
- [x] 7.2 Auth/catálogo/inscrição/avaliação/Lambdas inalterados nos contratos
- [x] 7.3 Checklist teardown (ECS desired=0 ou destroy, RDS stop, crons)
