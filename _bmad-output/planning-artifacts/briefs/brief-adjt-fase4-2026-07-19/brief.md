---
title: "Product Brief — Plataforma de Feedbacks (Tech Challenge Fase 4)"
status: draft
created: 2026-07-19
updated: 2026-07-20
project: adjt-fase4
author: ThiagoFerreira
---

# Product Brief: Plataforma de Feedbacks FIAP

## Executive Summary

Plataforma de feedback para cursos on-line: estudantes enviam avaliações de aulas; administradores recebem alertas de itens críticos e relatórios semanais com médias e distribuições. O produto nasce como entrega acadêmica do Tech Challenge da Fase 4 (Cloud Computing, Serverless e Deploy), com prazo **28/07/2026**, desenvolvido **solo** por ThiagoFerreira.

A solução combina uma API Java/Spring Boot hospedada na **AWS** com **funções serverless** para notificação e geração de relatórios — atendendo requisitos obrigatórios de cloud e serverless do enunciado. Prioridade explícita: **aprender AWS** mantendo **custo mínimo** (projeto acadêmico pago pelo próprio desenvolvedor). Demonstração via **vídeo gravado**, conforme orientação do desafio.

## The Problem

Instituições de ensino on-line precisam saber rapidamente se as aulas estão satisfazendo os alunos. Sem um canal estruturado de feedback e alertas automáticos, problemas críticos (notas baixas, reclamações urgentes) demoram a ser percebidos; relatórios manuais consomem tempo e não escalam.

Hoje, no contexto do desafio, o problema é simulado mas realista: garantir qualidade do curso exige **captura**, **triagem de urgência** e **visão agregada** dos feedbacks — não apenas armazenar comentários.

## The Solution

Uma aplicação cloud-native onde:

1. **Estudantes** enviam feedback via `POST /avaliação` com descrição e nota (0–10).
2. **O sistema** persiste a avaliação, classifica urgência por nota (`≤4` ALTA → alerta imediato; `5–7` MÉDIA; `≥8` BAIXA) e dispara fluxos assíncronos.
3. **Funções serverless** (mínimo duas, responsabilidade única):
   - **Notificação de urgência** — envia e-mail ao administrador com descrição, urgência e data.
   - **Relatório semanal** — consolida médias, quantidade por dia e por nível de urgência.
4. **Administradores** recebem alertas por e-mail e relatório semanal em **e-mail HTML + PDF no S3** — sem UI dedicada no MVP.

**Cliente da API:** Postman (login JWT → `POST /avaliação`). Demonstração via vídeo com chamadas manuais.

Experiência principal: API REST autenticada + automações serverless + observabilidade básica (logs, métricas, alarmes). Infra na AWS região **sa-east-1**.

## What Makes This Different

Não é um produto comercial — o diferencial aqui é **execução disciplinada dentro de restrições reais**:

- **Aprendizado AWS** com stack profissional (Spring Boot, JWT, PostgreSQL, mensageria).
- **Arquitetura cost-aware**: serverless onde faz sentido (eventos esporádicos), containers mínimos para a API, free tier e teardown pós-demo.
- **Separação clara de responsabilidades** — critério explícito de avaliação do desafio.

Moat técnico: nenhum (projeto acadêmico). Vantagem: clareza arquitetural e documentação que sustentam a nota e o aprendizado.

## Who This Serves

| Persona | Necessidade | Sucesso |
|---------|-------------|---------|
| **Estudante** | Avaliar aula de forma simples | Enviar feedback em uma chamada HTTP |
| **Administrador** | Saber de problemas críticos e tendências | Receber alerta imediato + relatório semanal legível |
| **Desenvolvedor (Thiago)** | Entregar desafio, aprender AWS, gastar pouco | Sistema funcional no vídeo, repo documentado, infra desligável |

## Success Criteria

**Funcionais (enunciado):**
- [ ] `POST /avaliação` aceita `{ descricao, nota }` (nota 0–10)
- [ ] ≥ 2 funções serverless com responsabilidade única
- [ ] Notificação automática para feedbacks críticos
- [ ] Relatório semanal com média de avaliações, qty por dia e por urgência
- [ ] Deploy automatizado dos componentes atualizáveis
- [ ] Monitoramento configurado

**Acadêmicos:**
- [ ] Vídeo demonstrando app, serverless e configurações
- [ ] Repo com código, arquitetura, deploy, monitoramento e funções documentados
- [ ] Entrega até **28/07/2026**

**Pessoais:**
- [ ] Infra AWS provisionada e explicada (modelo cloud escolhido)
- [ ] Custo mensal estimado abaixo de **USD 20–30** com teardown após entrega

## Scope

**In (MVP — entrega do desafio):**
- API Spring Boot 3.4.5 / Java 17 com **JWT completo** (Spring Security — login, emissão e validação)
- PostgreSQL (RDS ou local + RDS) com Flyway
- Mensageria: **SQS** em produção AWS; Kafka no Docker Compose para dev local
- API em **ECS Fargate** (sa-east-1)
- Resilience4j na API (circuit breaker/retry para integrações externas)
- 2+ Lambdas (notificação + relatório) + EventBridge cron semanal
- Relatório semanal: e-mail HTML (SES) + PDF no S3
- E-mail via SES (sandbox ou e-mail verificado para demo)
- Região AWS: **sa-east-1**
- Orçamento-alvo: **~USD 20–30/mês**
- Docker Compose para desenvolvimento local
- CI/CD básico (GitHub Actions → deploy AWS)
- CloudWatch logs/alarms

**Out (explicitamente fora do MVP):**
- Portal web para admin ou estudante
- MSK (Kafka gerenciado) em produção — custo proibitivo para projeto solo
- Multi-tenant / múltiplos cursos
- Alta disponibilidade ou escala além do necessário para demo
- Ambiente cloud 24/7 após entrega — infra deve ser desligável

## Vision

Se evoluísse além do acadêmico: dashboard admin, autenticação por perfil (estudante/admin), analytics em tempo real e integração com LMS. Para este ciclo, a visão é **dominar o fluxo completo** — código → cloud → serverless → observabilidade — com artefatos que servem de portfólio.
