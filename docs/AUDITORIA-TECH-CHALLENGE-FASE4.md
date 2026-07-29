# Auditoria final — Tech Challenge Fase 4

**Fonte:** [`ADJT - Fase 4 - Tech Challenge_rev.pdf`](./ADJT%20-%20Fase%204%20-%20Tech%20Challenge_rev.pdf)  
**Repo:** https://github.com/thiagohaf/adjt-fase4-feedback (público)  
**Data da auditoria:** 2026-07-28 (re-auditoria no mesmo dia)  
**Escopo:** conferência item a item do enunciado oficial contra o código, IaC, CI/CD e documentação deste repositório.

## Veredito

O projeto **continua apto à entrega acadêmica**: contempla o núcleo obrigatório do enunciado (app em nuvem, ≥2 funções serverless com SRP, notificação de urgência, relatório semanal com média e quantidades, deploy automatizado, monitoramento, repositório aberto e documentação de arquitetura/deploy/funções).

A re-auditoria **reconfirmou** os fechamentos anteriores (`POST /avaliacao`, SNS no alarme 5XX, lista no relatório, SG RDS endurecido) e registrou apenas **refinos documentais / precisão de inventário** (imagem API via CDK asset, nota stale de VPC no UJ-4 — corrigida nesta passagem). O único artefato de entrega ainda **fora do repo** é o vídeo no YouTube.

| Status | Significado |
|--------|-------------|
| **ATENDE** | Evidência clara no repo / código |
| **ATENDE PARCIALMENTE** | Cumpre a intenção, com desvio ou lacuna menor |
| **NÃO VERIFICÁVEL** | Exige evidência fora do repo (conta AWS live, vídeo publicado) |
| **NÃO ATENDE** | Lacuna material frente ao enunciado |

---

## 1. Enunciado — Problema e objetivo

| Item | Status | Evidência |
|------|--------|-----------|
| Plataforma de feedback (estudantes avaliam aulas; admins acompanham) | **ATENDE** | API Quarkus: catálogo, inscrição, avaliação; e-mails SES para admin |
| App hospedado em nuvem | **ATENDE** | ECS Fargate + ALB (`feedbacks/infra/.../ApiStack.java`); workflows AWS |
| Funções serverless para notificação e relatórios | **ATENDE** | `feedbacks/apps/notification`, `feedbacks/apps/report` |
| Demo em vídeo (créditos limitados) | **NÃO VERIFICÁVEL** | Roteiro em `docs/ROTEIRO-DEMO.md` (13 cenas); nenhum URL YouTube no repo |

---

## 2. Requisitos (lista do PDF)

| # | Requisito do enunciado | Status | Evidência | Observação |
|---|------------------------|--------|-----------|------------|
| R1 | Ambiente de nuvem configurado e funcionando, com segurança de dados e governança de acesso | **ATENDE** | JWT + papéis `ESTUDANTE`/`ADMINISTRADOR`; Secrets Manager (`feedbacks/jwt`, `feedbacks/db`, `feedbacks/adminEmail`); CDK IAM; SG RDS restrito a ECS + Lambda report (`harden-rds-sg`); alarme SNS | RDS permanece `publicly-accessible` (endpoint estável sem NAT). HTTPS no ALB se `FEEDBACKS_ACM_CERT_ARN` |
| R2 | Componentes de suporte (bancos de dados etc.) | **ATENDE** | RDS PostgreSQL `feedbacks-demo`; SQS+DLQ; S3; SES; EventBridge; Secrets Manager | RDS via `demo-lifecycle.sh` (não só CDK) |
| R3 | Deploy automatizado dos componentes atualizáveis | **ATENDE** | `.github/workflows/deploy-all.yml` — package Lambdas + `cdk deploy` das 3 stacks + imagem API (DockerImageAsset) + seed + smoke + `harden-rds-sg` | Disparo `workflow_dispatch` (manual), adequado ao ciclo de demo |
| R4 | Aplicação monitorada | **ATENDE** | Health `/api/v1/health` (+ `/q/health`); alarme `feedbacks-api-target-5xx` com **SNS** → e-mail admin | Confirmar subscription SNS uma vez no e-mail |
| R5 | Notificações automáticas aos administradores para problemas críticos | **ATENDE** | Avaliação ALTA → SQS → Lambda notification → SES; alarme 5XX → SNS | “Crítico” no domínio = urgência ALTA (nota ≤ 4); gate de publish na API |
| R6 | Relatório semanal dos feedbacks, com média de avaliações | **ATENDE** | Lambda report: média + qty/dia + qty/urgência + lista Descrição/Urgência/Data; EventBridge; HTML SES + PDF S3 | Diário e semanal |

---

## 3. Regras da aplicação

| Regra | Status | Evidência |
|-------|--------|-----------|
| Obrigatório implementar serverless | **ATENDE** | 2 Lambdas Quarkus (`notification`, `report`) |
| Obrigatório rodar em ambiente cloud | **ATENDE** | ECS Fargate + ALB em `us-east-1` |
| Mínimo 2 funções serverless com Responsabilidade Única (SRP) | **ATENDE** | `feedbacks-lambda-notification` — só alerta SES (sem JDBC/REST); `feedbacks-lambda-report` — só relatório JDBC read + SES/S3 (sem Flyway/REST); `ModuleConstraintsTest` em ambos |

---

## 4. Referências do enunciado — contratos de dados

### 4.1 Endpoint `POST /avaliação`

| Esperado (PDF) | Implementado | Status |
|----------------|--------------|--------|
| `POST /avaliação` | `POST /avaliacao` (ASCII, sem acento) | **ATENDE** |
| Body `{ "descricao": string, "nota": int (0–10) }` | Body `{ "aulaId", "descricao", "nota" }` com `@Min(0) @Max(10)` | **ATENDE PARCIALMENTE** |

**Evidência:** `AvaliacaoResource.java` (`@Path("/avaliacao")`), `CriarAvaliacaoRequest.java`.

**Justificativa:** path alinhado ao enunciado em ASCII (URLs sem acento). `aulaId` permanece obrigatório pelo domínio Curso/Aula; Postman documenta o contrato real.

### 4.2 E-mail de aviso de urgência

| Campo PDF | Status | Evidência |
|-----------|--------|-----------|
| Descrição | **ATENDE** | `ProcessAlertUseCase.buildBody` — `Descrição: %s` |
| Urgência | **ATENDE** | `Urgência: %s` |
| Data de envio | **ATENDE** | `Data (ocorridoEm): %s` |

Disparo apenas para urgência **ALTA** (nota ≤ 4) — publish gated em `CriarAvaliacaoUseCase`; Lambda consome a fila.

### 4.3 Dados do relatório semanal

| Campo / regra | Status | Evidência |
|---------------|--------|-----------|
| Média de avaliações (requisito R6) | **ATENDE** | HTML/PDF: `Média de notas` (`GenerateReportUseCase`, `ReportPdfGenerator`, `ReportAggregates`) |
| Quantidade de avaliações por dia | **ATENDE** | Tabela “Por dia civil” / `qtyPorDia` |
| Quantidade de avaliações por urgência | **ATENDE** | Tabela ALTA / MEDIA / BAIXA |
| Descrição; Urgência; Data de envio (lista da p.4 do PDF) | **ATENDE** | HTML/PDF do relatório (diário e semanal): tabela Descrição / Urgência / Data de envio |

**Extras além do mínimo:** relatório diário (mesmo Lambda), PDF no S3, invoke manual para demo.

---

## 5. Artefatos de entrega

| Artefato | Status | Evidência |
|----------|--------|-----------|
| Repositório aberto com código-fonte | **ATENDE** | `https://github.com/thiagohaf/adjt-fase4-feedback` — `visibility: PUBLIC` (confirmado via `gh` nesta re-auditoria) |
| Vídeo de demonstração (app + serverless + configurações) | **NÃO VERIFICÁVEL** | Roteiro completo em `docs/ROTEIRO-DEMO.md` (13 cenas); nenhum `youtube.com` / `youtu.be` no repo |

---

## 6. Critérios de avaliação (rubrica do PDF)

| Critério | Status | Evidência |
|----------|--------|-----------|
| Explicação do modelo de cloud e componentes | **ATENDE** | `README.md` (diagramas mermaid + stacks); cena 2 do `ROTEIRO-DEMO.md`; `feedbacks/infra/README.md` |
| Funcionamento correto da aplicação | **ATENDE** (código/testes) / **NÃO VERIFICÁVEL** (runtime live) | Testes JUnit/`@QuarkusTest`; Postman em `docs/postman/`; smoke health no deploy; ALB DNS no roteiro |
| Qualidade do código, com documentação | **ATENDE** | Hexagonal + Quarkus; OpenSpec/BMAD; JaCoCo gate ≥ 90% nos 3 `pom.xml` (api, notification, report); CI `test(api)` |
| Descrição: arquitetura; deploy; monitoramento; funções | **ATENDE** | README + `infra/README.md` + `ROTEIRO-DEMO.md` + `UJ4-ROTEIRO.md` + specs OpenSpec |
| Configuração cloud/serverless + modelo + segurança | **ATENDE** | CDK (3 stacks) + `harden-rds-sg` + SNS no alarme; HTTPS opcional via ACM |

---

## 7. Inventário técnico vs enunciado

### 7.1 Funções serverless (SRP)

| Função | Responsabilidade | Trigger | Saída |
|--------|------------------|---------|-------|
| `feedbacks-lambda-notification` | Alerta de urgência ALTA | SQS | SES (texto) |
| `feedbacks-lambda-report` | Relatório diário/semanal | EventBridge (+ invoke manual) | SES (HTML) + S3 (PDF) |

### 7.2 Stacks CDK

| Stack | Recursos |
|-------|----------|
| `FeedbacksAlertaNotificationStack` | SQS + DLQ → Lambda notification → SES |
| `FeedbacksRelatorioStack` | EventBridge diário/semanal → Lambda report (VPC default, subnet pública) → S3 + SES |
| `FeedbacksApiStack` | ECR repo `feedbacks-api` (evidência) + ECS Fargate + ALB + alarme 5XX → SNS; imagem de runtime via `DockerImageAsset` |

### 7.3 Esteiras GitHub Actions

| Workflow | Papel |
|----------|-------|
| `test(api)` | Qualidade + JaCoCo ≥ 90% |
| `deploy(all)` | Deploy completo da demo (RDS/secrets, Lambdas, 3 stacks, seed, smoke, harden SG) |
| `pause(demo)` | Corta custo horário |
| `destroy(all)` | Teardown |

### 7.4 Documentação de suporte à entrega

| Documento | Conteúdo |
|-----------|----------|
| `README.md` | Arquitetura, stack, local, Postman, AWS, CI/CD |
| `docs/ROTEIRO-DEMO.md` | Roteiro YouTube (13 cenas) + ALB DNS demo |
| `docs/como-rodar-intellij.md` | Setup local |
| `docs/postman/` | Collection + envs local/AWS |
| `feedbacks/infra/README.md` | CDK, secrets, invoke, VPC/SG |
| `feedbacks/apps/report/UJ4-ROTEIRO.md` | Relatórios sob demanda |

---

## 8. Desvios e riscos (não bloqueiam o núcleo)

Residuais após fechamento dos gaps de auditoria e reconfirmação:

| # | Item | Impacto | Recomendação para a entrega |
|---|------|---------|-----------------------------|
| 1 | Body exige `aulaId` além de `descricao`/`nota` | Baixo | Mencionar no vídeo: vínculo obrigatório à Aula |
| 2 | Região `us-east-1` (brief/PRD citavam `sa-east-1`) | Baixo | Já no roteiro (cena 2); justificar créditos/conta |
| 3 | HTTPS no ALB só com ACM (`FEEDBACKS_ACM_CERT_ARN`) | Baixo | Narrar HTTP como default de demo; HTTPS opcional se houver certificado |
| 4 | Vídeo YouTube | **Obrigatório** / **NÃO VERIFICÁVEL** no repo | Seguir `ROTEIRO-DEMO.md` e publicar; não há evidência no repo |
| 5 | Imagem da API no Fargate vem de `DockerImageAsset` (ECR asset do CDK), não do repositório nomeado `feedbacks-api` | Baixo (precisão de inventário) | No vídeo: ECR nomeado como artefato/evidência; deploy da imagem pelo CDK asset |

**Encerrados (não são mais gaps):** path `POST /avaliacao` (ATENDE); R1 SG RDS restrito a ECS + Lambda report; R4 alarme 5XX com SNS → e-mail admin; relatório com lista Descrição / Urgência / Data de envio; nota stale “Lambda fora de VPC” no UJ-4 (alinhada à VPC default do `RelatorioStack`).

**Pós-auditoria inicial (mesma data, reforçam entregabilidade):** fix CDK `redirectHTTP` (#42), SG descriptions ASCII (#43), refresh do `baseUrl` ALB no Postman/roteiro.

---

## 9. Resumo executivo

| Categoria | Resultado |
|-----------|-----------|
| Requisitos R1–R6 | **6/6 ATENDE** |
| Regras (serverless, cloud, SRP×2) | **ATENDE** |
| Contratos de dados (avaliação, alerta, relatório) | Path `/avaliacao` **ATENDE**; lista do relatório **ATENDE**; residual: `aulaId` no body |
| Artefatos (repo + vídeo) | Repo **ATENDE** (público confirmado); vídeo YouTube **NÃO VERIFICÁVEL** |
| Rubrica de avaliação | Documentação, modelo cloud e monitoramento **ATENDE**; residuais: `aulaId`, região, HTTPS só com ACM, nuance ECR asset |

**Conclusão:** o repositório permanece **apto à entrega acadêmica** do Tech Challenge Fase 4 quanto ao software, à nuvem e à documentação. O item que permanece fora desta auditoria de código é a **publicação do vídeo no YouTube**, para a qual o roteiro e o ambiente de demo já estão preparados.

---

## 10. Checklist rápido (copiar para a gravação)

- [ ] Repo público acessível
- [ ] `deploy(all)` verde / ALB com health UP (inclui `harden-rds-sg`)
- [ ] Postman: login → inscrição → `POST /avaliacao` (nota ≤ 4; body com `aulaId`)
- [ ] E-mail SES de alerta com Descrição / Urgência / Data
- [ ] SNS: subscription do alarme 5XX confirmada no e-mail admin (uma vez)
- [ ] Invoke relatório semanal + PDF no S3 + e-mail HTML (média + qty/dia + qty/urgência + **lista** Descrição/Urgência/Data)
- [ ] CloudWatch: métricas ALB + alarme `feedbacks-api-target-5xx` (ação SNS)
- [ ] Actions: `test(api)` / `deploy(all)`
- [ ] Explicar modelo cloud (ECS + Lambdas), SG RDS endurecido e HTTPS opcional (ACM)
- [ ] Vídeo publicado no YouTube
- [ ] `pause(demo)` ou `destroy(all)` após a gravação
