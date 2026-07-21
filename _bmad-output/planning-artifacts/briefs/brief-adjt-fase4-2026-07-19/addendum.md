# Addendum — Detalhes técnicos e decisões de infra

Conteúdo que não cabe no brief mas alimenta PRD e arquitetura.

## Stack confirmada

> **Superseded (2026-07-21):** a stack abaixo (Spring Boot) foi substituída por **Java 17 + Quarkus 3.33 LTS**
> pela Sprint Change Proposal aprovada em `_bmad-output/planning-artifacts/sprint-change-proposal-2026-07-21.md`.
> A tabela original é preservada como registro histórico da decisão de 2026-07-19; a stack vigente está no
> Architecture Spine e no addendum do PRD.


| Camada                | Tecnologia                                              |
| --------------------- | ------------------------------------------------------- |
| Linguagem / Framework | Java 17, Spring Boot 3.4.5                              |
| Segurança             | Spring Security, JWT completo                           |
| Persistência          | Spring Data JPA, PostgreSQL                             |
| Migrações             | Flyway                                                  |
| Mensageria            | Apache Kafka (dev local); **Amazon SQS** (produção AWS) |
| Resiliência           | Resilience4j (Circuit Breaker, Retry, Fallback)         |
| Infra local           | Docker, Docker Compose                                  |
| Cloud                 | **AWS** `sa-east-1` (objetivo de aprendizado)           |


## Referência do enunciado (PDF)

- Endpoint: `POST /avaliação` → `{ "descricao": string, "nota": int (0–10) }`
- E-mail de urgência: descrição, urgência, data de envio
- Relatório semanal: descrição, urgência, data, qty avaliações/dia, qty por urgência
- Obrigatório: serverless (≥2 funções, SRP), cloud, segurança/governança, deploy automatizado, monitoramento
- Entrega: repo + vídeo (não exige cloud 24/7 por créditos limitados)

Fonte: `docs/ADJT - Fase 4 - Tech Challenge_rev.pdf`

## Proposta AWS cost-aware (confirmada)

**Região:** `sa-east-1` (São Paulo)  
**Orçamento-alvo:** ~USD 20–30/mês, com teardown pós-entrega


| Componente            | Serviço                                | Motivo de custo                   |
| --------------------- | -------------------------------------- | --------------------------------- |
| API Spring Boot       | **ECS Fargate** (0.25 vCPU)            | Container familiar; evita MSK     |
| Banco                 | RDS PostgreSQL db.t3.micro (free tier) | Enunciado exige BD                |
| Serverless #1         | Lambda — notificação de urgência       | SRP; dispara por SQS              |
| Serverless #2         | Lambda — relatório semanal             | SRP; cron EventBridge             |
| Fila                  | **SQS**                                | Centavos; substitui Kafka em prod |
| E-mail                | SES                                    | Barato; sandbox OK para demo      |
| Artefatos / relatório | S3                                     | PDF do relatório semanal          |
| Secrets               | Secrets Manager ou SSM Parameter Store | JWT, credenciais BD               |
| Monitoramento         | CloudWatch Logs + Alarmes              | Requisito do desafio              |
| CI/CD                 | GitHub Actions + ECR                   | Deploy automatizado               |


**Kafka:** manter no Docker Compose para desenvolvimento e aprendizado local; em produção AWS, **SQS** reduz custo drasticamente vs MSK. Documentar a decisão no vídeo/arquitetura.

**Economia pós-entrega:** script ou checklist para parar RDS, escalar ECS a zero, remover EventBridge rules.

## Contexto do desenvolvedor

- **Solo** — sem grupo; todas as decisões concentradas em uma pessoa
- **Prazo:** 28/07/2026 (~9 dias a partir de 19/07)
- **Restrições extras:** nenhuma além do PDF
- **Prioridade:** aprender AWS + nota do desafio + custo mínimo

## Regras de urgência (confirmadas)

- `nota <= 4` → urgência **ALTA** → **alerta imediato** (e-mail)
- `nota 5–7` → **MÉDIA**
- `nota >= 8` → **BAIXA**
- Notificação imediata apenas para ALTA

## Decisões confirmadas (prontas para o PRD)


| #   | Decisão           | Escolha                                                              |
| --- | ----------------- | -------------------------------------------------------------------- |
| 1   | Autenticação      | **JWT completo** (Spring Security — login, emissão e validação)      |
| 2   | Cliente da API    | **Postman** (demonstração e testes; sem frontend)                    |
| 3   | Relatório semanal | **Ambos** — e-mail HTML ao administrador + PDF no S3                 |
| 4   | Região AWS        | **`sa-east-1`** (São Paulo)                                          |
| 5   | Urgência          | nota ≤4 = ALTA (alerta imediato); 5–7 MÉDIA; ≥8 BAIXA                |
| 6   | Mensageria prod   | **SQS** (Kafka só no Docker Compose local)                           |
| 7   | API cloud         | **ECS Fargate**                                                      |
| 8   | Orçamento         | **~USD 20–30/mês** com teardown após entrega                         |
