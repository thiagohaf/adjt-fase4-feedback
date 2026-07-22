# Proposal — Implementar Módulo 02: Catálogo Curso/Aula (Quarkus)

> **Fonte de verdade funcional:** `openspec/modules/02-catalogo-curso-aula/` (proposal.md, specs.md, design.md).
> Esta change **deriva** desses documentos — não os reinventa. Auth (módulo 01) é contrato estável —
> não alterar claims JWT, códigos `AUTH_*` nem login. Spine: AD-2, AD-8, AD-9, AD-14.

## Why

Com o módulo 01 entregue, a API autentica e autoriza papéis, mas o catálogo ainda é stub:
`CursoResource` / `AulaResource` devolvem dados inventados e a Aula usa `titulo`, divergente do
PRD (`nome`). Sem domínio e persistência reais, Bruno (Admin) não monta o catálogo demonstrável
(UJ-2), Ana (Estudante) não tem Cursos/Aulas estáveis, e Inscrição/Avaliação ficam bloqueados.

## What Changes

- Substitui stubs de catálogo por domínio real `Curso` / `Aula` (hexágono: api → application →
  domain ← infrastructure), mantendo paths e `@RolesAllowed` já usados no módulo 01.
- **BREAKING** (apenas contrato provisório do stub): campo JSON da Aula passa de `titulo` para
  `nome` (PRD FR-3 / Spine); atualizar DTOs e collection Postman.
- Cria Curso (`POST /api/v1/cursos`) e Aula (`POST /api/v1/cursos/{cursoId}/aulas`) — Admin;
  `nome` obrigatório; `descricao` opcional.
- Lista/consulta: `GET /api/v1/cursos`, **novo** `GET /api/v1/cursos/{id}`,
  `GET /api/v1/cursos/{cursoId}/aulas`, `GET /api/v1/aulas/{id}` — Admin e Estudante.
- Persistência Flyway **somente V3** (`curso` / `aula`); não reescrever V1/V2 de auth; sem seed
  de catálogo no MVP.
- Erros de domínio: 404 `CURSO_NOT_FOUND` / `AULA_NOT_FOUND`; 400 `VALIDATION_ERROR`;
  códigos `AUTH_*` intactos.
- Stub de inscrição (`POST .../inscricoes`) permanece intocado; sem update/delete de catálogo.
- Testes cobrindo specs do módulo; JaCoCo ≥ 90% no código deste módulo (AD-14).

## Capabilities

### New Capabilities

- `catalog-curso`: criar, listar e consultar Curso (FR-3/FR-4; SPEC-3.1–3.4, SPEC-4.1–4.3,
  SPEC-NFR-C*); envelope de erro e persistência do agregado Curso.
- `catalog-aula`: criar, listar e consultar Aula com campo `nome` (não `titulo`); exige Curso
  existente (FR-3/FR-4; SPEC-3.5–3.9, SPEC-4.4–4.7).

### Modified Capabilities

<!-- Auth paths/@RolesAllowed permanecem; só o corpo stub muda — sem delta de requisito AUTH_*. -->

## Impact

- **Código:** `feedbacks/apps/api/` — substituir stubs em `CursoResource` / `AulaResource`;
  adicionar domain/application/infrastructure de catálogo; ExceptionMappers para
  `CursoNotFoundException` / `AulaNotFoundException`; `V3__create_catalogo.sql`.
- **Não tocar:** `AuthResource`, claims JWT, mapa `AUTH_*`, V1/V2 Flyway, stub de inscrição.
- **Docs:** `docs/postman/feedbacks-api.postman_collection.json` (`nome` na Aula;
  `GET /cursos/{id}` se faltar).
- **Branch sugerida:** `feature/openspec-02-catalogo-curso-aula-quarkus` a partir de `develop`;
  PR para `develop`.
- **Fora de escopo:** Inscrição (FR-5/6), Avaliação, alertas/relatórios, update/delete/seed de
  catálogo, reabrir Spring→Quarkus.
