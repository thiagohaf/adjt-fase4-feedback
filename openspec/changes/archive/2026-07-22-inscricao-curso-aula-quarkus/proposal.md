# Proposal — Implementar Módulo 03: Inscrição Curso/Aula (Quarkus)

> **Fonte de verdade funcional:** `openspec/modules/03-inscricao-curso-aula/` (proposal.md, specs.md, design.md).
> Esta change **deriva** desses documentos — não os reinventa. Auth (módulo 01) e catálogo
> (módulo 02) são contratos estáveis — não alterar claims JWT, códigos `AUTH_*`, login, nem
> schema/contrato de Curso/Aula. Spine: AD-2, AD-3, AD-8, AD-9, AD-14, AD-15.

## Why

Com o módulo 02 entregue, o catálogo persiste `cursoId`/`aulaId` reais, mas a inscrição ainda é
stub: `POST .../inscricoes` devolve `INSCRITO` sem vínculo. Sem inscrição em Curso e Aula (FR-5)
nem gate de Avaliação (FR-6), a UJ-1 não fecha e o módulo Avaliação fica bloqueado (AD-15).

## What Changes

- Substitui o stub `POST /api/v1/cursos/{cursoId}/inscricoes` por domínio real `InscricaoCurso`
  (hexágono), mantendo path e `@RolesAllowed("ESTUDANTE")`.
- Adiciona `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` para `InscricaoAula`,
  exigindo inscrição prévia no Curso e Aula pertencente ao Curso.
- Persistência Flyway **somente V4** (`inscricao_curso` / `inscricao_aula` com UNIQUE);
  não reescrever V1–V3.
- Duplicatas → **409** `INSCRICAO_DUPLICADA`; pré-condição Curso→Aula → **409**
  `INSCRICAO_CURSO_OBRIGATORIA`; ids inexistentes reusam `CURSO_NOT_FOUND` / `AULA_NOT_FOUND`.
- Gate FR-6: porta `VerificarInscricaoAula` + rejeição no stub `POST /api/v1/avaliacoes` sem
  inscrição (`INSCRICAO_AULA_OBRIGATORIA`); **sem** implementar Avaliação completa (FR-7).
- Testes cobrindo specs do módulo; JaCoCo ≥ 90% no código deste módulo (AD-14).
- Atualiza collection Postman (inscrição Curso + Aula no fluxo UJ-1).

## Capabilities

### New Capabilities

- `inscricao-curso`: Estudante se inscreve em Curso existente; duplicata 409; Admin 403
  (FR-5; SPEC-5.1–5.4, SPEC-5.11).
- `inscricao-aula`: Estudante se inscreve em Aula do Curso após inscrição no Curso; regras
  de pertencimento/duplicata/pré-condição (FR-5; SPEC-5.5–5.10).
- `inscricao-gate-avaliacao`: Verificação de inscrição na Aula e rejeição no stub de
  `POST /avaliacoes` (FR-6; SPEC-6.1–6.3).

### Modified Capabilities

<!-- Auth e catálogo: paths/roles e códigos CURSO_*/AULA_*/AUTH_* permanecem — sem delta de requisito. -->

## Impact

- **Código:** `feedbacks/apps/api/` — substituir stub em `CursoResource.inscrever`; novo
  resource/DTO de inscrição em Aula; domain/application/infrastructure de enrollment;
  ExceptionMappers para exceções de inscrição; gate em `AvaliacaoResource`; 
  `V4__create_inscricao.sql`.
- **Não tocar:** `AuthResource`, claims JWT, mapa `AUTH_*`, V1–V3 Flyway, contrato Curso/Aula.
- **Docs:** `docs/postman/feedbacks-api.postman_collection.json` (inscrição Curso real +
  inscrição Aula).
- **Branch:** `feature/openspec-03-inscricao-curso-aula` a partir de `develop`; PR para `develop`.
- **Fora de escopo:** Avaliação FR-7+, listagem HTTP de inscrição, cancelamento, alertas/relatórios.
