# Auditoria final — Tech Challenge Fase 4

**Fonte:** [`ADJT - Fase 4 - Tech Challenge_rev.pdf`](./ADJT%20-%20Fase%204%20-%20Tech%20Challenge_rev.pdf)  
**Repo:** https://github.com/thiagohaf/adjt-fase4-feedback (público)  
**Data da auditoria:** 2026-07-28  
**Escopo:** conferência item a item do enunciado oficial contra o código, IaC, CI/CD e documentação deste repositório.

## Veredito

O projeto **contempla o núcleo obrigatório** do enunciado: aplicação em nuvem, ≥2 funções serverless com responsabilidade única, notificação de urgência, relatório semanal com média e quantidades, deploy automatizado, monitoramento, repositório aberto e documentação de arquitetura/deploy/funções.

Há **desvios conscientes e documentados** (path versionado da API, região `us-east-1`, trade-offs de rede da demo) e **um artefato de entrega externo ao repo** (vídeo no YouTube), coberto por roteiro mas não verificável aqui.

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
| Demo em vídeo (créditos limitados) | **NÃO VERIFICÁVEL** | Roteiro em `docs/ROTEIRO-DEMO.md`; vídeo YouTube não está no repo |

---

## 2. Requisitos (lista do PDF)

| # | Requisito do enunciado | Status | Evidência | Observação |
|---|------------------------|--------|-----------|------------|
| R1 | Ambiente de nuvem configurado e funcionando, com segurança de dados e governança de acesso | **ATENDE PARCIALMENTE** | JWT + papéis (`ESTUDANTE`/`ADMINISTRADOR`); Secrets Manager (`feedbacks/jwt`, `feedbacks/db`, `feedbacks/adminEmail`); CDK com IAM | Rede da demo é permissiva: RDS `--publicly-accessible` + SG `0.0.0.0/0:5432` (`demo-lifecycle.sh`); ALB HTTP :80 sem TLS; ECS com IP público. Trade-off de custo/demo — narrar no vídeo |
| R2 | Componentes de suporte (bancos de dados etc.) | **ATENDE** | RDS PostgreSQL `feedbacks-demo`; SQS+DLQ; S3; SES; EventBridge; Secrets Manager | RDS provisionado via script de ciclo de vida (não só CDK) |
| R3 | Deploy automatizado dos componentes atualizáveis | **ATENDE** | `.github/workflows/deploy-all.yml` — package Lambdas + `cdk deploy` das 3 stacks + imagem API | Disparo `workflow_dispatch` (manual), adequado ao ciclo de demo |
| R4 | Aplicação monitorada | **ATENDE PARCIALMENTE** | Health `/api/v1/health`; alarme CloudWatch `feedbacks-api-target-5xx` (`ApiStack.java`); cenas no roteiro | Alarme **sem** ação SNS (visível no console, sem e-mail automático de 5XX) |
| R5 | Notificações automáticas aos administradores para problemas críticos | **ATENDE** | Avaliação ALTA → SQS → Lambda notification → SES | “Crítico” no domínio = urgência ALTA (nota ≤ 4). Alarme de infra não notifica por e-mail |
| R6 | Relatório semanal dos feedbacks, com média de avaliações | **ATENDE** | Lambda report: média + qty/dia + qty/urgência; EventBridge semanal; HTML SES + PDF S3 | Ver §4 sobre campos literais do PDF |

---

## 3. Regras da aplicação

| Regra | Status | Evidência |
|-------|--------|-----------|
| Obrigatório implementar serverless | **ATENDE** | 2 Lambdas Quarkus (`notification`, `report`) |
| Obrigatório rodar em ambiente cloud | **ATENDE** | ECS Fargate + ALB em `us-east-1` |
| Mínimo 2 funções serverless com Responsabilidade Única (SRP) | **ATENDE** | `feedbacks-lambda-notification` — só alerta SES (sem JDBC/REST); `feedbacks-lambda-report` — só relatório (sem Flyway/REST); testes de constraint de módulo |

---

## 4. Referências do enunciado — contratos de dados

### 4.1 Endpoint `POST /avaliação`

| Esperado (PDF) | Implementado | Status |
|----------------|--------------|--------|
| `POST /avaliação` | `POST /api/v1/avaliacoes` | **ATENDE PARCIALMENTE** |
| Body `{ "descricao": string, "nota": int (0–10) }` | Body `{ "aulaId", "descricao", "nota" }` com `@Min(0) @Max(10)` | **ATENDE PARCIALMENTE** |

**Evidência:** `AvaliacaoResource.java` (`@Path("/api/v1/avaliacoes")`), `CriarAvaliacaoRequest.java`.

**Justificativa:** versionamento `/api/v1` e `aulaId` são necessários ao domínio Curso/Aula do produto; o Postman e o README documentam o contrato real. Citar no vídeo que o enunciado foi atendido com path versionado e vínculo à aula.

### 4.2 E-mail de aviso de urgência

| Campo PDF | Status | Evidência |
|-----------|--------|-----------|
| Descrição | **ATENDE** | `ProcessAlertUseCase.buildBody` — `Descrição: %s` |
| Urgência | **ATENDE** | `Urgência: %s` |
| Data de envio | **ATENDE** | `Data (ocorridoEm): %s` |

Disparo apenas para urgência **ALTA** (nota ≤ 4).

### 4.3 Dados do relatório semanal

| Campo / regra | Status | Evidência |
|---------------|--------|-----------|
| Média de avaliações (requisito R6) | **ATENDE** | HTML/PDF: `Média de notas` (`GenerateReportUseCase`, `ReportPdfGenerator`) |
| Quantidade de avaliações por dia | **ATENDE** | Tabela “Por dia civil” / `qtyPorDia` |
| Quantidade de avaliações por urgência | **ATENDE** | Tabela ALTA / MEDIA / BAIXA |
| Descrição; Urgência; Data de envio (lista da p.4 do PDF) | **ATENDE PARCIALMENTE** | Esses três campos estão no **e-mail de alerta**. O relatório é **agregado** (média + volumes), não lista avaliações individuais |

**Leitura adotada (alinhada ao PRD):** a lista “Descrição / Urgência / Data” na seção do relatório semanal do PDF parece ecoar o bloco do alerta; o requisito explícito de relatório é média + qty/dia + qty/urgência — todos presentes. Se a banca exigir listagem individual no e-mail semanal, isso **não** está implementado.

**Extras além do mínimo:** relatório diário (mesmo Lambda), PDF no S3, invoke manual para demo.

---

## 5. Artefatos de entrega

| Artefato | Status | Evidência |
|----------|--------|-----------|
| Repositório aberto com código-fonte | **ATENDE** | `https://github.com/thiagohaf/adjt-fase4-feedback` — `visibility: PUBLIC` |
| Vídeo de demonstração (app + serverless + configurações) | **NÃO VERIFICÁVEL** | Roteiro completo em `docs/ROTEIRO-DEMO.md` (13 cenas); link YouTube fora do escopo do repo |

---

## 6. Critérios de avaliação (rubrica do PDF)

| Critério | Status | Evidência |
|----------|--------|-----------|
| Explicação do modelo de cloud e componentes | **ATENDE** | `README.md` (diagrama mermaid + stacks); cena 2 do `ROTEIRO-DEMO.md`; `feedbacks/infra/README.md` |
| Funcionamento correto da aplicação | **ATENDE** (código/testes) / **NÃO VERIFICÁVEL** (runtime live) | Testes JUnit/`@QuarkusTest`; Postman em `docs/postman/`; smoke health no deploy |
| Qualidade do código, com documentação | **ATENDE** | Hexagonal + Quarkus; OpenSpec/BMAD; JaCoCo gate ≥ 90% nos 3 `pom.xml`; CI `test(api)` |
| Descrição: arquitetura; deploy; monitoramento; funções | **ATENDE** | README + `infra/README.md` + `ROTEIRO-DEMO.md` + `UJ4-ROTEIRO.md` + specs OpenSpec |
| Configuração cloud/serverless + modelo + segurança | **ATENDE PARCIALMENTE** | CDK (3 stacks) + scripts + secrets/JWT documentados; segurança de rede da demo é o ponto fraco (ver R1) |

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
| `FeedbacksRelatorioStack` | EventBridge diário/semanal → Lambda report → S3 + SES |
| `FeedbacksApiStack` | ECR + ECS Fargate + ALB + alarme 5XX |

### 7.3 Esteiras GitHub Actions

| Workflow | Papel |
|----------|-------|
| `test(api)` | Qualidade + JaCoCo ≥ 90% |
| `deploy(all)` | Deploy completo da demo |
| `pause(demo)` | Corta custo horário |
| `destroy(all)` | Teardown |

### 7.4 Documentação de suporte à entrega

| Documento | Conteúdo |
|-----------|----------|
| `README.md` | Arquitetura, stack, local, Postman, AWS, CI/CD |
| `docs/ROTEIRO-DEMO.md` | Roteiro YouTube |
| `docs/como-rodar-intellij.md` | Setup local |
| `docs/postman/` | Collection + envs local/AWS |
| `feedbacks/infra/README.md` | CDK, secrets, invoke |
| `feedbacks/apps/report/UJ4-ROTEIRO.md` | Relatórios sob demanda |

---

## 8. Desvios e riscos (não bloqueiam o núcleo)

| # | Item | Impacto | Recomendação para a entrega |
|---|------|---------|-----------------------------|
| 1 | Path `POST /api/v1/avaliacoes` ≠ `POST /avaliação` | Baixo (se explicado) | Mencionar no vídeo: versionamento REST + `aulaId` |
| 2 | Região `us-east-1` (brief/PRD citavam `sa-east-1`) | Baixo | Já no roteiro (cena 2); justificar créditos/conta |
| 3 | Alarme 5XX sem SNS | Baixo | Mostrar alarme no CloudWatch; opcional: adicionar SNS depois |
| 4 | RDS público / HTTP sem TLS | Médio na rubrica de segurança | Narrar como trade-off de demo (evitar NAT/custo); teardown após nota |
| 5 | Vídeo YouTube | **Obrigatório** | Seguir `ROTEIRO-DEMO.md` e publicar; não há evidência no repo |
| 6 | Leitura literal dos campos do relatório (Descrição/Urgência/Data) | Baixo/médio | Se a banca for literal, incluir amostra no HTML; hoje o agregado cobre R6 |

---

## 9. Resumo executivo

| Categoria | Resultado |
|-----------|-----------|
| Requisitos R1–R6 | **6/6 cobertos**; R1 e R4 parciais (segurança de rede / alarme sem ação) |
| Regras (serverless, cloud, SRP×2) | **ATENDE** |
| Contratos de dados (avaliação, alerta, relatório) | **ATENDE** com desvio de path/body; relatório agregado completo |
| Artefatos (repo + vídeo) | Repo **ATENDE**; vídeo **NÃO VERIFICÁVEL** |
| Rubrica de avaliação | Documentação e modelo cloud **ATENDE**; segurança de rede da demo é o principal gap narrativo |

**Conclusão:** o repositório está **apto à entrega acadêmica** do Tech Challenge Fase 4 quanto ao software, à nuvem e à documentação. O item que permanece fora desta auditoria de código é a **publicação do vídeo no YouTube**, para a qual o roteiro e o ambiente de demo já estão preparados.

---

## 10. Checklist rápido (copiar para a gravação)

- [ ] Repo público acessível
- [ ] `deploy(all)` verde / ALB com health UP
- [ ] Postman: login → inscrição → `POST /api/v1/avaliacoes` (nota ≤ 4)
- [ ] E-mail SES de alerta com Descrição / Urgência / Data
- [ ] Invoke relatório semanal + PDF no S3 + e-mail HTML (média + qty/dia + qty/urgência)
- [ ] CloudWatch: métricas ALB + alarme `feedbacks-api-target-5xx`
- [ ] Actions: `test(api)` / `deploy(all)`
- [ ] Explicar modelo cloud (ECS + Lambdas) e trade-offs de segurança da demo
- [ ] Vídeo publicado no YouTube
- [ ] `pause(demo)` ou `destroy(all)` após a gravação
