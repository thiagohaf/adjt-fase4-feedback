# Tasks — Implementar Módulo 03: Inscrição Curso/Aula (Quarkus)

> Referências: specs desta change (`specs/inscricao-*/spec.md`), design desta change (`design.md`,
> D1–D7) e o design do módulo `openspec/modules/03-inscricao-curso-aula/design.md`.

## 1. Branch e baseline

- [x] 1.1 Criar branch `feature/openspec-03-inscricao-curso-aula` a partir de `develop` atualizado
- [x] 1.2 Confirmar stub atual (`CursoResource.inscrever`), `AvaliacaoResource.criar`, ExceptionMappers e Flyway V1–V3 intactos — baseline

## 2. Domínio e exceções

- [x] 2.1 Criar `domain/enrollment/{InscricaoCurso,InscricaoAula}.java` (ids UUID, estudanteId, cursoId/aulaId, criadoEm) conforme design do módulo §4
- [x] 2.2 Criar `domain/exception/{InscricaoDuplicadaException,InscricaoCursoObrigatoriaException,InscricaoAulaObrigatoriaException}.java` com códigos `INSCRICAO_DUPLICADA` / `INSCRICAO_CURSO_OBRIGATORIA` / `INSCRICAO_AULA_OBRIGATORIA`

## 3. Application — portas e use cases

- [x] 3.1 Criar portas `application/enrollment/port/{InscricaoCursoRepository,InscricaoAulaRepository}.java` (save + existsBy…)
- [x] 3.2 Implementar `InscreverEmCursoUseCase` (Curso existe; duplicata → exceção; `estudanteId` via CurrentUserProvider)
- [x] 3.3 Implementar `InscreverEmAulaUseCase` (Curso + Aula do Curso + inscrição Curso; duplicata Aula)
- [x] 3.4 Implementar `VerificarInscricaoAulaUseCase` (porta do gate FR-6 / SPEC-6.3)

## 4. Infraestrutura — JPA + Flyway V4

- [x] 4.1 Criar `V4__create_inscricao.sql` (`inscricao_curso` / `inscricao_aula`, FKs, UNIQUEs, índices); **não** alterar V1–V3; sem seed
- [x] 4.2 Implementar Entities, repositórios JPA e adapters `JpaInscricaoCursoRepository` / `JpaInscricaoAulaRepository`; mapear UNIQUE violation → `InscricaoDuplicadaException`

## 5. Camada web — resources, DTOs e erros

- [x] 5.1 Criar DTOs `InscricaoCursoResponse` / `InscricaoAulaResponse` (sem body de create)
- [x] 5.2 Substituir stub `CursoResource.inscrever` por use case real; manter path e `@RolesAllowed("ESTUDANTE")`
- [x] 5.3 Adicionar `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` com `@RolesAllowed("ESTUDANTE")`
- [x] 5.4 Registrar ExceptionMappers 409/403 dos códigos de inscrição; reutilizar mappers 404 de catálogo; não alterar mapa `AUTH_*`
- [x] 5.5 Gate FR-6 em `AvaliacaoResource.criar`: exigir `aulaId`; sem inscrição → 403 `INSCRICAO_AULA_OBRIGATORIA`; com inscrição → stub 201 (sem FR-7)

## 6. Testes (cada SPEC → teste)

- [x] 6.1 Unit: `InscreverEmCursoUseCaseTest`, `InscreverEmAulaUseCaseTest`, `VerificarInscricaoAulaUseCaseTest`
- [x] 6.2 `@QuarkusTest` inscrição Curso: SPEC-5.1–5.4, 401 (SPEC-5.11)
- [x] 6.3 `@QuarkusTest` inscrição Aula: SPEC-5.5–5.10, 401 regressão
- [x] 6.4 `@QuarkusTest` gate Avaliação: SPEC-6.1 / 6.2
- [x] 6.5 Adaptar testes de auth SPEC-2.8/2.9 que batem no stub sem Curso — arrange com Curso/Aula reais; asserts `AUTH_*` intactos

## 7. Postman, cobertura e critério de saída

- [x] 7.1 Atualizar `docs/postman/feedbacks-api.postman_collection.json`: inscrição Curso real; request inscrição Aula; fluxo UJ-1 (Curso → Aula → avaliação gate)
- [x] 7.2 Rodar `mvn verify` — suíte verde; JaCoCo ≥ 90% no código de produção deste módulo (AD-14)
- [x] 7.3 Conferir checkboxes do proposal.md §6 do módulo 03; auth e catálogo inalterados
