# Proposal — Implementar Módulo 04: Avaliação de Aula (Quarkus)

> **Fonte de verdade funcional:** `openspec/modules/04-avaliacao-aula/` (proposal.md, specs.md, design.md).
> Esta change **deriva** desses documentos — não os reinventa. Auth (01), catálogo (02) e
> inscrição/gate FR-6 (03) são contratos estáveis — não alterar claims JWT, códigos `AUTH_*`,
> login, schema Curso/Aula, nem regras de inscrição. Spine: AD-2, AD-3, AD-4, AD-5, AD-7,
> AD-8, AD-9, AD-14, AD-15, AD-16.

## Why

Com inscrição e gate FR-6 entregues, `POST /api/v1/avaliacoes` ainda é stub: devolve 201 com
UUID aleatório sem persistir `nota`/Urgência nem aplicar unicidade Estudante+Aula. Sem FR-7–FR-9
a UJ-1 não fecha, o read model de relatório (AD-16) não tem dados e o alerta ALTA (FR-10) fica
sem evento. É o próximo módulo da cadeia após CI `ci(api)` e inscrição arquivada.

## What Changes

- Substitui o stub `POST /api/v1/avaliacoes` por domínio real `Avaliacao` (hexágono):
  `descricao` + `nota` (0–10) + `aulaId`; `estudanteId` = `sub` do JWT.
- Reusa o gate FR-6 (`VerificarInscricaoAulaUseCase`); sem inscrição → 403
  `INSCRICAO_AULA_OBRIGATORIA` (inalterado).
- Persiste Avaliação com Urgência derivada (ALTA ≤4 / MÉDIA 5–7 / BAIXA ≥8) e
  `ocorridoEm`; Flyway **somente V5** (evolui tabela `avaliacao` de V1 — não reescrever V1–V4).
- Unicidade `(estudanteId, aulaId)` → **409** `AVALIACAO_DUPLICADA` (sem upsert; AD-15).
- Após commit, se Urgência ALTA → publica evento via porta `EvaluationEventPublisher`
  (contrato AD-5); falha de publish **não** reverte a Avaliação (AD-4). Lambda/SES (FR-10)
  fora de escopo.
- Enriquece `GET /api/v1/avaliacoes` com `nota`, `urgencia`, `ocorridoEm`, vínculos de
  catálogo (`aulaId`/`cursoId`), mantendo filtro Admin vs próprias (FR-8 / SPEC-2.10–2.11).
- Testes cobrindo specs do módulo; JaCoCo ≥ 90% no código deste módulo (AD-14).
- Atualiza collection Postman (UJ-1 até Avaliação real; nota de fronteira ALTA).

## Capabilities

### New Capabilities

- `avaliacao-criar`: Estudante inscrito cria Avaliação com `descricao`/`nota`/`aulaId`;
  validação 0–10; persistência + unicidade 409; Aula inexistente 404 (FR-7; SPEC-7.x).
- `avaliacao-urgencia`: Classificação de Urgência na persistência; fronteiras 4/5/7/8
  (FR-9; SPEC-9.x).
- `avaliacao-listar`: Listagem com nota, Urgência, data e vínculos; escopo por papel
  (FR-8; SPEC-8.x) — complementa SPEC-2.10/2.11 sem alterar `AUTH_*`.
- `avaliacao-evento-alerta`: Após commit ALTA, publica JSON AD-5 via
  `EvaluationEventPublisher` (Kafka `%local` / adapter AWS preparado); MÉDIA/BAIXA não
  publicam; falha de publish não apaga Avaliação (AD-4/AD-5/AD-7).

### Modified Capabilities

- `inscricao-gate-avaliacao`: SPEC-6.2 deixa de descrever “stub 201 provisório”; após o gate,
  o fluxo continua para criação real de Avaliação (FR-7). Gate 403 / porta de verificação
  (SPEC-6.1, SPEC-6.3) permanecem.

## Impact

- **Código:** `feedbacks/apps/api/` — domain/application/infrastructure de Avaliação;
  substituir stub em `AvaliacaoResource`; DTOs reais; ExceptionMappers; Flyway
  `V5__evolve_avaliacao.sql`; porta + adapters de publish; enriquecer read model de listagem.
- **Não tocar:** `AuthResource`, claims JWT, mapa `AUTH_*`, V1–V4 Flyway (exceto V5 aditiva
  sobre `avaliacao`), contrato Curso/Aula/Inscrição, Lambdas SES/relatório.
- **Docs:** `docs/postman/feedbacks-api.postman_collection.json` (POST Avaliação real + UJ-1).
- **Branch:** `feature/openspec-04-avaliacao-quarkus` a partir de `develop`; PR para `develop`.
- **Fora de escopo:** Lambda de alerta/SES (FR-10), relatórios (FR-11+), update/delete de
  Avaliação, upsert, CI deploy ECR/CDK (FR-15 restante).
