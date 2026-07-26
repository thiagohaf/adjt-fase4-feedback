# Design — FR-13/14/15/16: Health, ECS mínimo, métricas e roteiro

## Context

API Quarkus roda local/`%test`; stacks CDK cobrem só alerta e relatório. RDS demo
`feedbacks-demo` é público (exceção AD-17). Secrets `feedbacks/db` e
`feedbacks/adminEmail` existem; fila SQS de alerta já tem URL. `/api/v1/health`
sempre retorna UP; CI `ci-api.yml` só faz `mvn verify`. Prazo vídeo: 28/07.

## Goals / Non-Goals

**Goals:**
- Health com checagem de DB filmável (FR-13 / AD-11).
- API em ECS Fargate + ALB com métricas e ≥1 alarme (FR-14 / AD-1 / AD-11).
- Pipeline: build → imagem ECR → evidência de deploy CDK (FR-15 / AD-12).
- Roteiro único YouTube (FR-16 / PRD §11).

**Non-Goals:**
- VPC privada + NAT (alvo AD-17 completo).
- Multi-AZ, Autoscale avançado, blue/green.
- Alterar Lambdas, domínio HTTP ou SES sandbox.
- Portal web.

## Decisions

### D1 — Health: SmallRye + contrato `/api/v1/health`

- **Escolha:** Usar `quarkus-smallrye-health` (já no pom) com check de DB
  (Agroal/`@Readiness` ou check embutido datasource). `/api/v1/health` agrega o
  status de readiness (UP só se DB ok); `/q/health` permanece para probes ALB.
- **Alternativa:** Só Actuator-like custom sem SmallRye — rejeitada (AD-11).
- **Alternativa:** `/api/v1/health` sempre UP e DB só em `/q/health/ready` —
  rejeitada para a cena Postman (PRD cena 3 usa “GET health”).

### D2 — Envelope demo: ECS público + RDS público

- **Escolha:** Task ECS em subnet pública com IP público **ou** ALB → task em
  subnet pública (sem NAT). SG: ALB:80/443 → ECS:8080; ECS → RDS:5432 (mesmo
  padrão da Lambda report fora de VPC).
- **Sizing:** 0.25 vCPU / 512 MB; desiredCount=1; JVM container Quarkus.
- **Alternativa:** NAT + privados — fora do orçamento/prazo da conta demo.

### D3 — Imagem: Dockerfile JVM Quarkus

- **Escolha:** `Dockerfile` multi-stage (Maven package → `eclipse-temurin:17-jre`)
  com `quarkus.package.jar.type=uber-jar` ou `fast-jar` + layout Quarkus.
- **Alternativa:** Native GraalVM — mais lento no CI; adiar.
- **Alternativa:** Jib sem Dockerfile — ok, mas Dockerfile é mais filmável no vídeo.

### D4 — Secrets e env na task

- **Escolha:** Task recebe `QUARKUS_PROFILE=aws`, `JWT_SECRET`, `DB_URL`,
  `DB_USERNAME`, `DB_PASSWORD`, `FEEDBACKS_SQS_ALERT_QUEUE_URL` a partir de
  Secrets Manager (`feedbacks/jwt` ou chave `jwtSecret` no secret existente) +
  `feedbacks/db` + output da stack de alerta.
- Preferir `secrets` do ECS a partir do Secrets Manager (não plaintext no
  task definition) quando viável; se o padrão atual das Lambdas (unwrap no CDK)
  for reutilizado por prazo, documentar como dívida e teardown obrigatório.

### D5 — Observabilidade FR-14

- **Escolha:** Métricas nativas do ALB (`RequestCount`, `HTTPCode_Target_5XX_Count`,
  `TargetResponseTime`) + alarme CloudWatch: `HTTPCode_Target_5XX_Count > 0` em
  5 minutos (ou threshold baixo filmável). Health check ALB em `/q/health/ready`.
- **Alternativa:** Micrometer → CloudWatch agent — overhead desnecessário para MVP.

### D6 — Pipeline FR-15

- **Escolha:** Manter `ci-api.yml` (verify). Novo workflow `deploy-api.yml`
  (`workflow_dispatch` + push `develop` opcional): build imagem → push ECR →
  `cdk deploy FeedbacksApiStack` (OIDC ou secrets AWS já usados na conta).
- Evidência no vídeo: Actions verde + task RUNNING + ALB DNS.

### D7 — Roteiro FR-16

- **Escolha:** Um arquivo `docs/ROTEIRO-DEMO.md` com tabela de cenas (PRD §11),
  links para Postman, UJ4-ROTEIRO, console AWS (métricas/alarme/CI). Não duplicar
  o conteúdo longo do UJ-4 — referenciar.

## Risks / Trade-offs

| Risco | Mitigação |
| --- | --- |
| Custo ECS+ALB+RDS juntos | desiredCount=1; stop RDS + desired=0 após gravação; checklist teardown |
| RDS público + ECS público | SG restrito onde possível; teardown pós-vídeo; não é produção |
| Secrets em env (padrão Lambda) | Preferir ECS secrets; se unwrap, documentar dívida |
| Cold start / health fail no boot | Grace period ALB; Flyway no start; probe em `/q/health/ready` |
| Conta sem OIDC GHA | `workflow_dispatch` com credenciais documentadas; deploy local `cdk` como fallback filmável |
| Região spine `sa-east-1` vs demo `us-east-1` | Manter `us-east-1` da conta demo; justificar no roteiro cena 2 |

## Migration Plan

1. Implementar health + testes → merge parcial possível.
2. Dockerfile + synth `ApiStack` → `cdk deploy` com imagem placeholder/local.
3. Workflow GHA push ECR + redeploy.
4. Criar alarme; gerar tráfego Postman; capturar evidência.
5. Escrever `ROTEIRO-DEMO.md`.
6. Rollback: `cdk destroy FeedbacksApiStack`; ECR lifecycle; RDS stop.

## Open Questions

1. Nome do secret JWT na conta (`feedbacks/jwt` vs chave em secret existente) —
   confirmar no apply.
2. HTTPS no ALB (cert ACM) vs HTTP-only demo — default HTTP para velocidade;
   documentar.
