---
title: "PRD — Plataforma de Feedbacks FIAP"
status: final
created: 2026-07-20
updated: 2026-07-20
project: adjt-fase4
author: ThiagoFerreira
inputs:
  - "_bmad-output/planning-artifacts/briefs/brief-adjt-fase4-2026-07-19/"
---

# PRD: Plataforma de Feedbacks FIAP

## 0. Document Purpose

Este PRD orienta a implementação e a entrega do Tech Challenge Fase 4 (Cloud Computing, Serverless e Deploy) por **ThiagoFerreira** (desenvolvimento **solo**, sem grupo). Serve de contrato de capacidades para arquitetura, epics/stories e o roteiro do vídeo no YouTube. Vocabulário ancorado no Glossário; funcionalidades com FRs numerados globalmente; inferências marcadas com `[ASSUMPTION]` e listadas no índice. Baseia-se no Product Brief em `_bmad-output/planning-artifacts/briefs/brief-adjt-fase4-2026-07-19/` — decisões de stack e infra ficam no `addendum.md` deste workspace, não aqui.

## 1. Vision

A Plataforma de Feedbacks FIAP permite que **Estudantes** avaliem **Aulas** de **Cursos** on-line com nota e descrição, e que **Administradores** descubram problemas críticos na hora e acompanhem a qualidade agregada no **Relatório diário** e no **Relatório semanal** — sem portal web no MVP: tudo via API REST (Postman) e e-mail.

O produto resolve a lacuna entre “guardar comentários” e **agir**: cada Avaliação é classificada por urgência; notas baixas disparam alerta imediato; relatórios periódicos consolidam médias e volumes. Nasce como entrega acadêmica com prazo **28/07/2026**, priorizando aprendizado AWS, custo baixo e demonstração gravada.

Se evoluísse além do desafio: dashboard admin, integração LMS e analytics em tempo real. Neste ciclo, a visão é o fluxo completo — API autenticada → cloud → serverless → observabilidade — documentado e filmável.

## 2. Target User

### 2.1 Jobs To Be Done

- **Estudante:** matricular-se no Curso/Aula e registrar feedback em uma chamada autenticada.
- **Administrador:** estruturar catálogo (Curso/Aula), consultar o que existe, ser avisado de Avaliações críticas sem abrir a API o tempo todo, e receber Relatório diário e Relatório semanal.
- **Desenvolvedor (Thiago):** provar no vídeo (e no repo) que cloud, serverless, segurança, deploy e monitoramento funcionam — gastando pouco e podendo desligar a infra depois.

### 2.2 Non-Users (v1)

- Usuários finais via browser/app nativo (não há UI).
- Instituições reais multi-tenant / SSO / LMS.
- Operação 24/7 pós-entrega.

### 2.3 Key User Journeys

- **UJ-1. Ana (Estudante) avalia uma Aula após se inscrever.**  
  Ana faz login JWT como Estudante, lista Cursos/Aulas disponíveis, inscreve-se no Curso e na Aula, depois envia Avaliação (`descricao` + `nota`) vinculada à Aula. Recebe confirmação HTTP; se a nota for ≤4, o Administrador será alertado por e-mail (Ana não vê o alerta). Realiza FR-1, FR-4–FR-7.

- **UJ-2. Bruno (Administrador) monta o catálogo e consulta.**  
  Bruno faz login JWT como Administrador, cria Curso e Aulas, lista/consulta o catálogo e as Avaliações quando precisa evidenciar no Postman. Realiza FR-1–FR-4, FR-8.

- **UJ-3. Bruno recebe alerta de urgência ALTA.**  
  Após uma Avaliação com nota ≤4, Bruno recebe e-mail (SES, endereço verificado) com descrição, urgência e data — sem precisar consultar a API. Realiza FR-9–FR-10.

- **UJ-4. Bruno recebe o Relatório diário e o Relatório semanal.**  
  Nos horários agendados, Bruno recebe e-mail HTML com agregados do período (dia ou semana) e pode abrir o PDF correspondente no S3. Realiza FR-11–FR-12, FR-17.

- **UJ-5. Thiago grava o vídeo de entrega.**  
  Thiago segue o roteiro (§11): health check, métricas, fluxo aluno/admin, alerta, PDF no S3, deploy e monitoramento — sem se perder na gravação. Realiza SM acadêmicos e FR-13–FR-15.

## 3. Glossary

- **Estudante** — Papel autenticado via JWT que se inscreve em Curso/Aula e cria Avaliação.
- **Administrador** — Papel autenticado via JWT que cria/consulta Curso e Aula, consulta Avaliações, e é o destinatário dos e-mails de alerta e relatório. `[ASSUMPTION: um único Administrador de demo; e-mail = endereço verificado no SES.]`
- **Curso** — Unidade de catálogo criada pelo Administrador; contém uma ou mais Aulas.
- **Aula** — Unidade avaliável dentro de um Curso; alvo da Avaliação e da inscrição do Estudante.
- **Inscrição** — Vínculo do Estudante a um Curso e a uma Aula que autoriza criar Avaliação daquela Aula. `[ASSUMPTION: inscrição em Curso e em Aula são ambas obrigatórias antes de avaliar.]`
- **Avaliação** — Feedback com `descricao` (texto) e `nota` (inteiro 0–10), sempre vinculada a uma Aula (e, por consequência, a um Curso).
- **Urgência** — Classificação derivada da nota: **ALTA** (≤4), **MÉDIA** (5–7), **BAIXA** (≥8).
- **Alerta de urgência** — E-mail imediato ao Administrador disparado apenas para Urgência ALTA.
- **Relatório diário** — Consolidado do dia civil anterior (média de notas, quantidade total de Avaliações, quantidade por nível de Urgência), entregue por e-mail HTML e como PDF em armazenamento de objetos. `[ASSUMPTION: não exige série “qty por dia” multi-dia; o dia é a própria janela.]`
- **Relatório semanal** — Consolidado dos 7 dias civis anteriores (média de notas, quantidade de Avaliações por dia, quantidade por nível de Urgência), entregue por e-mail HTML e como PDF em armazenamento de objetos. Exigência do enunciado; o Relatório diário é aditivo.
- **API** — Superfície REST autenticada; cliente oficial do MVP é o Postman.
- **Health check** — Endpoint (ou conjunto) que indica se a API está apta a receber tráfego.
- **Métricas de mercado** — Indicadores operacionais comuns em APIs cloud (ex.: latência, taxa de erro, contagem de requisições, utilização de recurso) expostos/observáveis para a demo. `[ASSUMPTION: conjunto mínimo = request count, error rate/4xx-5xx, latency p95 (ou equivalente CloudWatch), e saúde do serviço.]`

## 4. Features

### 4.1 Autenticação e papéis

**Description:** Login emite JWT; a API valida o token e autoriza por papel (Estudante vs Administrador). Realizes UJ-1, UJ-2.

**Functional Requirements:**

#### FR-1: Login e emissão de JWT

Ator autenticável (Estudante ou Administrador) pode obter JWT via endpoint de login com credenciais válidas.

**Consequences (testable):**
- Credenciais válidas → token JWT utilizável nas rotas protegidas.
- Credenciais inválidas → rejeição sem emitir token.
- Token inválido/expirado → acesso negado às rotas protegidas.

#### FR-2: Autorização por papel

A API distingue ações de Estudante e Administrador com base no JWT.

**Consequences (testable):**
- Estudante não cria Curso/Aula.
- Administrador não precisa se inscrever para consultar catálogo/Avaliações.
- Rotas de criação de Avaliação exigem papel Estudante (ou equivalente autorizado).

### 4.2 Catálogo: Curso e Aula

**Description:** Administrador cria e consulta Cursos e Aulas; listagens disponíveis também ao Estudante para inscrição. Realizes UJ-2, UJ-1.

**Functional Requirements:**

#### FR-3: Administrador cria Curso e Aula

Administrador pode criar Curso e, dentro dele, Aulas.

**Consequences (testable):**
- Curso exige **nome** obrigatório; **descrição** opcional. `[ASSUMPTION: sem outros campos obrigatórios no MVP.]`
- Aula exige **nome** obrigatório e referência a Curso existente; **descrição** opcional; falha se o Curso não existir.
- Curso/Aula criados são listáveis/consultáveis por id.

#### FR-4: Listagem e consulta de catálogo

Estudante e Administrador podem listar/consultar Cursos e Aulas.

**Consequences (testable):**
- Endpoints de listagem/consulta retornam representações estáveis (id, dados essenciais).
- Consulta de id inexistente → resposta de não encontrado.

### 4.3 Inscrição do Estudante

**Description:** Antes de avaliar, o Estudante se inscreve no Curso e na Aula. Realizes UJ-1.

**Functional Requirements:**

#### FR-5: Inscrição em Curso e Aula

Estudante autenticado pode inscrever-se em um Curso e em uma Aula desse Curso.

**Consequences (testable):**
- Inscrição duplicada é rejeitada ou é idempotente de forma documentada. `[ASSUMPTION: rejeitar duplicata com erro de conflito.]`
- Inscrição em Aula de Curso no qual o Estudante não está inscrito é rejeitada.

#### FR-6: Avaliação exige inscrição

Estudante só cria Avaliação de Aula na qual está inscrito.

**Consequences (testable):**
- Sem inscrição → criação de Avaliação rejeitada.
- Com inscrição válida → criação permitida (demais regras de FR-7).

### 4.4 Avaliação de Aula

**Description:** Estudante envia `descricao` + `nota` (0–10) vinculada à Aula; o sistema persiste e calcula Urgência. Realizes UJ-1. Atende o enunciado `POST /avaliação`.

**Functional Requirements:**

#### FR-7: Criar Avaliação

Estudante inscrito pode criar Avaliação com `descricao` e `nota` (0–10) para uma Aula.

**Consequences (testable):**
- `nota` fora de 0–10 → rejeição de validação.
- Avaliação persistida inclui Urgência derivada e referência à Aula/Curso.
- Payload mínimo alinhado ao enunciado; contexto de Aula/Curso via path, query ou corpo. `[ASSUMPTION: `aulaId` (ou equivalente) no path ou body além de descricao/nota.]`
- No máximo **uma** Avaliação por par Estudante+Aula; segunda tentativa → conflito. `[ASSUMPTION: sem upsert; para corrigir, fora do MVP.]`

#### FR-8: Listagem/consulta de Avaliações

Administrador pode listar/consultar Avaliações; `[ASSUMPTION: Estudante pode listar apenas as próprias Avaliações.]`

**Consequences (testable):**
- Administrador vê Avaliações com nota, Urgência, data e vínculos de catálogo.
- Acesso não autorizado é rejeitado.

### 4.5 Alerta de urgência (serverless)

**Description:** Urgência ALTA dispara Alerta de urgência assíncrono por e-mail ao Administrador. Função serverless com responsabilidade única. Realizes UJ-3.

**Functional Requirements:**

#### FR-9: Classificação de Urgência

Ao persistir Avaliação, o sistema atribui Urgência: ALTA ≤4; MÉDIA 5–7; BAIXA ≥8.

**Consequences (testable):**
- Notas de fronteira (4, 5, 7, 8) classificam conforme a tabela acima.
- Urgência fica disponível para consulta e para o Relatório diário e o Relatório semanal.

#### FR-10: Notificação imediata ALTA

Para Urgência ALTA, o sistema envia Alerta de urgência ao e-mail do Administrador (SES verificado) contendo descrição, urgência e data de envio.

**Consequences (testable):**
- Avaliação ALTA → e-mail enviado (ou evidência demonstrável na demo: log + mensagem SES).
- MÉDIA/BAIXA → não disparam Alerta de urgência imediato.
- Processamento em função serverless separada da API (SRP).

### 4.6 Relatórios periódicos (serverless)

**Description:** Jobs periódicos geram Relatório diário e Relatório semanal (agregados do período), enviam HTML por e-mail e gravam PDF acessível no S3. Função(ões) serverless distintas do alerta, com responsabilidade de geração de relatório. Realizes UJ-4.

**Functional Requirements:**

#### FR-11: Geração do Relatório semanal

Em agendamento semanal, o sistema consolida Avaliações do período semanal.

**Consequences (testable):**
- Relatório semanal inclui: média de notas; quantidade de Avaliações por dia; quantidade por nível de Urgência.
- `[ASSUMPTION: janela = 7 dias civis anteriores ao disparo, fuso America/Sao_Paulo; disparo segunda 08:00 America/Sao_Paulo.]`
- Processamento serverless separado do Alerta de urgência (SRP).

#### FR-17: Geração do Relatório diário

Em agendamento diário, o sistema consolida Avaliações do dia civil anterior.

**Consequences (testable):**
- Relatório diário inclui: média de notas do dia; quantidade total de Avaliações do dia; quantidade por nível de Urgência.
- `[ASSUMPTION: disparo diário 08:00 America/Sao_Paulo; janela = dia civil anterior no mesmo fuso.]`
- Não substitui o Relatório semanal (ambos coexistem).

#### FR-12: Entrega HTML + PDF

Cada Relatório diário e Relatório semanal é enviado por e-mail HTML ao Administrador e o PDF correspondente fica disponível no S3 para abertura na demo.

**Consequences (testable):**
- E-mail HTML legível com os agregados e identificação do tipo (diário vs semanal) e do período.
- PDF recuperável no S3 (console ou URL demonstrável no vídeo), distinguível por nome/caminho de objeto.

### 4.7 Observabilidade e saúde

**Description:** Health check e métricas de mercado para provar monitoramento no vídeo. Realizes UJ-5.

**Functional Requirements:**

#### FR-13: Health check

A API expõe Health check utilizável na demo e por probes.

**Consequences (testable):**
- Resposta de “up” quando a API está saudável.
- Falha de dependência crítica refletida de forma documentada. `[ASSUMPTION: health inclui checagem básica de conectividade com o banco.]`

#### FR-14: Métricas observáveis

Sistema publica/exibe métricas de mercado (contagem de requests, erros, latência e/ou equivalentes CloudWatch) suficientes para a gravação.

**Consequences (testable):**
- No vídeo é possível mostrar ao menos um painel/alarma/métrica com tráfego gerado na demo.
- Existe alarme ou configuração de monitoramento documentada (requisito do enunciado).

### 4.8 Entrega acadêmica: deploy e roteiro

**Description:** Deploy automatizado dos componentes atualizáveis; roteiro de vídeo para YouTube. Realizes UJ-5.

**Functional Requirements:**

#### FR-15: Deploy automatizado

Componentes atualizáveis (no mínimo a API e as funções serverless relevantes) possuem pipeline de deploy automatizado.

**Consequences (testable):**
- Pipeline documentado e executável (evidência no vídeo ou logs de CI).
- Artefatos versionados (imagem/pacote) publicados no fluxo de deploy.

#### FR-16: Roteiro de demonstração

Existe roteiro (§11) cobrindo app, serverless, health/métricas, PDF no S3 e configurações — para gravação contínua no YouTube.

**Consequences (testable):**
- Roteiro lista cenas na ordem, comandos/chamadas Postman e o que a câmera deve mostrar.
- Tempo-alvo total cabe em vídeo acadêmico. `[ASSUMPTION: 8–15 minutos.]`

## 5. Non-Goals (Explicit)

- Portal web ou app mobile para Estudante/Administrador.
- Multi-tenant institucional, SSO, integração LMS/LTI.
- Kafka gerenciado (MSK) em produção.
- Alta disponibilidade / multi-AZ além do necessário para demo.
- Infra cloud 24/7 após a entrega (teardown é esperado).
- IA de sentimento, SMS, Slack, branching de survey, CRM.
- E-mail para destinatários fora do SES sandbox / não verificados (salvo saída futura do sandbox).

## 6. MVP Scope

### 6.1 In Scope

- API REST + JWT (Estudante e Administrador) via Postman.
- CRUD/consulta de Curso e Aula (criação admin; listagem/consulta).
- Inscrição de Estudante em Curso e Aula.
- Criação de Avaliação (`descricao`, `nota` 0–10) com Urgência.
- Listagem/consulta de Avaliações.
- ≥2 funções serverless SRP: Alerta de urgência + geração de relatório (diário e semanal).
- Relatório diário e Relatório semanal: e-mail HTML + PDF no S3.
- Health check + métricas/alarmes de monitoramento.
- Deploy automatizado + documentação no repo.
- Roteiro de vídeo YouTube (§11).
- Região e custo conforme Constraints (§10); teardown pós-demo.

### 6.2 Out of Scope for MVP

- UI web (razão: enunciado aceita demo API; prazo curto).
- MSK / multi-região / HA (custo e complexidade).
- Múltiplos Administradores com inboxes distintas (um e-mail SES basta).
- Refresh-token sofisticado / federação de identidade.
- Relatórios ad-hoc sob demanda além dos jobs diário/semanal (exceto trigger manual de demo se necessário). `[ASSUMPTION: permitido disparo manual one-shot do job de relatório (diário ou semanal) só para gravar o vídeo sem esperar a cron.]`

## 7. Success Metrics

**Primary**

- **SM-1**: Enunciado coberto — POST Avaliação, ≥2 serverless SRP, alerta ALTA, relatório semanal (média + qty/dia + qty/urgência), deploy automatizado, monitoramento; **mais** Relatório diário (aditivo). Valida FR-7, FR-9–FR-15, FR-17.
- **SM-2**: Vídeo no YouTube segue o roteiro e mostra health, métricas, alerta, PDF no S3 e **justificativa do modelo cloud** escolhido até **28/07/2026**. Valida FR-13, FR-14, FR-16.
- **SM-3**: Repo documenta arquitetura (incluindo modelo cloud), deploy, monitoramento e funções. Valida entrega acadêmica.

**Secondary**

- **SM-4**: Custo estimado de operação de demo ≤ **USD 20–30/mês**, com checklist de teardown. Valida Constraints de custo.
- **SM-5**: Fluxo UJ-1 e UJ-2 reproduzível no Postman em menos de 10 minutos de setup (seed + tokens).

**Counter-metrics (do not optimize)**

- **SM-C1**: Não otimizar para QPS/HA — risco de estourar custo e prazo. Counterbalances qualquer tentação de “produção enterprise”.
- **SM-C2**: Não inflar número de microserviços/Lambdas além do SRP exigido — counterbalance de “arquitetura para o currículo”.

## 8. Cross-Cutting NFRs

- **Segurança:** JWT obrigatório em rotas de negócio; segredos fora do código; papéis enforced server-side.
- **Resiliência:** falhas transitórias em integrações externas (e-mail/fila) não corrompem a Avaliação já persistida; retry/circuit conforme addendum.
- **Observabilidade:** logs correlacionáveis por request; métricas e ao menos um alarme útil na demo.
- **Performance (demo):** `[ASSUMPTION: p95 da API < 2s sob carga de demo; sem SLO de produção.]` Cold start ocasional de Lambda é tolerado.
- **Operação:** infra desligável; região `sa-east-1`.

## 9. API Surface (MVP)

Contratos lógicos (paths finais na arquitetura/OpenAPI). Autenticação: `Authorization: Bearer <JWT>` salvo login/health públicos.

| Capacidade | Ator | Notas |
|------------|------|-------|
| Login | — | Emite JWT com papel |
| Health check | — | FR-13 |
| Criar/listar/consultar Curso | Admin (escrever); ambos (ler) | FR-3, FR-4 |
| Criar/listar/consultar Aula | Admin (escrever); ambos (ler) | FR-3, FR-4 |
| Inscrever-se em Curso/Aula | Estudante | FR-5 |
| Criar Avaliação | Estudante | FR-7 — enunciado `POST /avaliação` |
| Listar/consultar Avaliações | Admin (+ próprias do Estudante) | FR-8 |
| *(assíncrono)* Alerta ALTA | Sistema → e-mail Admin | FR-10 |
| *(assíncrono)* Relatório diário | Sistema → e-mail + S3 | FR-17, FR-12 |
| *(assíncrono)* Relatório semanal | Sistema → e-mail + S3 | FR-11, FR-12 |

Breaking changes: evitados no ciclo do desafio; se inevitáveis, atualizar Postman collection + roteiro no mesmo PR.

## 10. Constraints and Guardrails

- **Prazo:** entrega até **28/07/2026**.
- **Equipe:** desenvolvimento solo (sem grupo) — escopo e rigor calibrados a uma pessoa.
- **Custo:** alvo ~USD 20–30/mês, privilegiando free tier onde couber; teardown após entrega (checklist + script/documentação de desligamento).
- **E-mail:** SES com destinatário verificado; sandbox OK para demo.
- **Form-factor:** API + e-mail + objeto S3; sem UI.
- **Aprendizado:** preferir escolhas AWS explicáveis no vídeo, inclusive o **modelo cloud** (container API + serverless para eventos) — ver addendum.
- **Créditos/cloud:** não manter 24/7 após a nota.

## 11. Roteiro de Demonstração (YouTube)

Ordem sugerida para não se perder na gravação. Ajustar timestamps na edição.

| # | Cena | O que mostrar | Evidência de requisito |
|---|------|---------------|------------------------|
| 1 | Abertura (30–45s) | Problema → solução em uma frase; stack em um slide | Contexto acadêmico |
| 2 | Arquitetura (1–2 min) | Diagrama API → fila → Lambdas; S3; SES; região sa-east-1; **por que** este modelo cloud (API em container + Lambdas para alerta/relatório) | Cloud + serverless + modelo escolhido |
| 3 | Health check | GET health “up” no Postman | FR-13 |
| 4 | Auth | Login Admin + login Estudante; tokens | FR-1, FR-2 |
| 5 | Catálogo | Admin cria Curso + Aula; listagens | FR-3, FR-4 |
| 6 | Inscrição + Avaliação OK | Estudante inscreve e avalia nota alta/média | FR-5–FR-7 |
| 7 | Avaliação crítica | Nota ≤4; mostrar resposta API | FR-7, FR-9 |
| 8 | Alerta | Caixa de e-mail do Admin com Alerta de urgência | FR-10 |
| 9 | Relatórios | Disparo (cron ou manual de demo) do diário e/ou semanal; e-mail HTML | FR-11, FR-12, FR-17 |
| 10 | PDF no S3 | Abrir ao menos um PDF (diário ou semanal) no console S3 / URL | FR-12 |
| 11 | Métricas | CloudWatch (requests, erros, latência) + alarme | FR-14 |
| 12 | Deploy | Pipeline CI/CD verde / deploy recente | FR-15 |
| 13 | Encerramento | Custo, teardown, link do repo | SM-3, SM-4 |

**Checklist pré-gravação:** e-mail SES verificado; seeds de usuários; Postman collection; um PDF de relatório gerado; alarmes visíveis; modo “não gastar” (escalas mínimas).

## 12. Open Questions

1. ~~Campos mínimos de Curso/Aula~~ → **Resolvido:** nome obrigatório; descrição opcional (FR-3).
2. ~~Re-avaliação da mesma Aula~~ → **Resolvido:** no máximo uma Avaliação por Estudante+Aula (FR-7).
3. Disparo manual do Relatório diário/semanal para a gravação — expor endpoint admin ou invocar Lambda no console? `[NOTE FOR PM: risco de demo se depender só da cron — decidir na arquitetura antes de gravar.]`
4. Path final do enunciado (`/avaliação` vs versionamento `/api/v1/...`)?
5. Conteúdo exato do PDF (tabelas vs gráfico) além dos agregados obrigatórios?

*OQ-3–5: dono ThiagoFerreira; revisitar em `bmad-architecture` / setup do vídeo. Não bloqueiam o PRD.*

## 13. Assumptions Index

- Administrador único; e-mail = verificado no SES. (§3, FR-10)
- Inscrição em Curso **e** Aula obrigatória antes de Avaliar. (§3, FR-5–FR-6)
- Duplicata de inscrição → conflito. (FR-5)
- Curso/Aula: só nome obrigatório (+ descrição opcional). (FR-3)
- `aulaId` (ou equivalente) no contrato de criação de Avaliação. (FR-7)
- Uma Avaliação por par Estudante+Aula; sem upsert no MVP. (FR-7)
- Estudante lista só as próprias Avaliações. (FR-8)
- Janela do Relatório semanal: 7 dias civis; cron segunda 08:00 America/Sao_Paulo. (FR-11)
- Relatório diário: dia civil anterior; cron diário 08:00 America/Sao_Paulo; métricas = média + qty total + qty/urgência. (FR-17)
- Métricas mínimas de mercado: request count, erros, latência, saúde. (§3, FR-14)
- Health inclui checagem básica de banco. (FR-13)
- Vídeo 8–15 min. (FR-16)
- Disparo manual one-shot do relatório (diário ou semanal) permitido só para demo. (§6.2)
- p95 API < 2s sob carga de demo (sem SLO de produção). (§8)
