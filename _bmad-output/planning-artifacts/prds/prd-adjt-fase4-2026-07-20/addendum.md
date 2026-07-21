# Addendum — PRD Plataforma de Feedbacks FIAP

Detalhes técnicos, stack e decisões de mecanismo que não pertencem ao PRD de capacidades. Alimenta `bmad-architecture` e o vídeo.

## Mudança vs Product Brief

| Tema | Brief | PRD (confirmado com Thiago) |
|------|-------|------------------------------|
| Domínio | Avaliação genérica `descricao`+`nota` | Avaliação com contexto **Curso/Aula**; inscrição do Estudante; Admin cria catálogo |
| “Múltiplos cursos” | Listado como OUT (multi-tenant) | Catálogo com N Cursos/Aulas **no mesmo tenant de demo** está IN |
| Auth na API | JWT (papel implícito) | JWT **Estudante + Administrador** explícitos |
| Consultas | Implícito mínimo | Endpoints de **listagem/consulta** IN |
| Demo | App + serverless + configs | + **health check**, **métricas de mercado**, **PDF no S3**, **roteiro YouTube** |
| Relatórios | Só semanal (enunciado) | **Diário + semanal** (semanal permanece; diário aditivo) |

## Stack (do brief/addendum do brief)

> Atualizado em 2026-07-21 pela Sprint Change Proposal (Spring Boot → Quarkus) — ver
> `_bmad-output/planning-artifacts/sprint-change-proposal-2026-07-21.md`.

| Camada | Tecnologia |
|--------|------------|
| Linguagem / Framework | Java 17, Quarkus 3.33 LTS |
| Segurança | Quarkus Security + SmallRye JWT (HS256) |
| Persistência | Hibernate ORM (Panache), PostgreSQL, Flyway |
| Mensageria | Kafka (Docker Compose local); **SQS** (produção AWS) |
| Resiliência | SmallRye Fault Tolerance |
| API cloud | ECS Fargate (0.25 vCPU), `sa-east-1` |
| Serverless | Lambda notificação; Lambda relatório (período diário\|semanal); EventBridge crons |
| E-mail / arquivos | SES; PDF no S3 |
| Secrets | Secrets Manager ou SSM |
| CI/CD | GitHub Actions + ECR |
| Monitoramento | CloudWatch Logs + Alarmes |

## Fluxo assíncrono (mecanismo)

1. API persiste Avaliação e publica evento/mensagem (SQS) quando Urgência = ALTA.
2. Lambda de notificação consome a fila e envia e-mail SES (descrição, urgência, data).
3. EventBridge com **duas regras** (cron diário + cron semanal) invoca a Lambda de relatório com parâmetro de período; consolida; SES HTML + PDF no S3 (objetos distinguíveis por período).

`[ASSUMPTION de mecanismo]` Preferir **uma** Lambda de relatório parametrizada (`diario` | `semanal`) para preservar SRP e ≥2 funções no total com a de notificação. Alternativa (3ª Lambda só-diário) fica aberta na arquitetura se a faculdade exigir funções 1:1 com jobs.

Kafka permanece só no Compose local para aprendizado; produção usa SQS (custo).

## Orçamento e teardown

- Alvo: ~USD 20–30/mês; privilegiar free tier (ex.: RDS db.t3.micro) onde couber.
- Pós-entrega: checklist **e** script/documentação para parar RDS, escalar ECS a zero, desabilitar regras EventBridge.
- Modelo cloud a narrar no vídeo: API em **container** (ECS Fargate) + **serverless** (Lambdas) para alerta e relatório — ver tabela de stack acima.

## Referência do enunciado

Fonte citada no brief: `docs/ADJT - Fase 4 - Tech Challenge_rev.pdf` — endpoint `POST /avaliação`, conteúdo mínimo de e-mail/relatório, obrigações cloud/serverless/deploy/monitoramento.
