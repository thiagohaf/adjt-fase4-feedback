# Tasks — Implementar Módulo 02: Catálogo Curso/Aula (Quarkus)

> Referências: specs desta change (`specs/catalog-*/spec.md`), design desta change (`design.md`,
> D1–D6) e o design do módulo `openspec/modules/02-catalogo-curso-aula/design.md`.

## 1. Branch e baseline

- [ ] 1.1 Criar branch `feature/openspec-02-catalogo-curso-aula-quarkus` a partir de `develop` atualizado
- [ ] 1.2 Confirmar stubs atuais (`CursoResource`, `AulaResource`), ExceptionMappers de auth e Flyway V1/V2 intactos — baseline para substituição

## 2. Domínio e exceções

- [ ] 2.1 Criar `domain/catalog/{Curso,Aula}.java` (id UUID, nome, descricao nullable, criadoEm; Aula com cursoId) conforme design do módulo §5
- [ ] 2.2 Criar `domain/exception/{CursoNotFoundException,AulaNotFoundException}.java` estendendo o padrão `DomainException` do módulo 01; mapear códigos `CURSO_NOT_FOUND` / `AULA_NOT_FOUND`

## 3. Application — portas e use cases

- [ ] 3.1 Criar portas `application/catalog/port/{CursoRepository,AulaRepository}.java` (save, findById, findAll / findByCursoId)
- [ ] 3.2 Implementar `CriarCursoUseCase`, `ListarCursosUseCase`, `ConsultarCursoUseCase`
- [ ] 3.3 Implementar `CriarAulaUseCase` (exige Curso ou `CursoNotFoundException`), `ListarAulasDoCursoUseCase`, `ConsultarAulaUseCase`

## 4. Infraestrutura — JPA + Flyway V3

- [ ] 4.1 Criar `V3__create_catalogo.sql` (tabelas `curso`/`aula`, FK, índice `idx_aula_curso_id`); **não** alterar V1/V2; sem seed
- [ ] 4.2 Implementar `CursoEntity`, `AulaEntity`, repositórios JPA e adapters `JpaCursoRepository` / `JpaAulaRepository` (listagem `criado_em ASC`)

## 5. Camada web — resources, DTOs e erros

- [ ] 5.1 Criar DTOs `CriarCursoRequest` / `CursoResponse` / `CriarAulaRequest` / `AulaResponse` com campo **`nome`** (não `titulo`); `@NotBlank` em `nome`
- [ ] 5.2 Substituir stub de `CursoResource`: POST criar, GET listar, **GET por id**; manter `@RolesAllowed`; manter stub `POST .../inscricoes` intocado
- [ ] 5.3 Substituir stub de `AulaResource` / criar-aula em `CursoResource`: POST/GET aulas do curso, GET aula por id; JSON só com `nome`
- [ ] 5.4 Registrar ExceptionMappers para `CursoNotFoundException`→404 `CURSO_NOT_FOUND` e `AulaNotFoundException`→404 `AULA_NOT_FOUND`; não alterar mapa `AUTH_*`

## 6. Testes (cada SPEC → teste)

- [ ] 6.1 Unit: `CriarCursoUseCaseTest`, `CriarAulaUseCaseTest` (sucesso + curso ausente), `Consultar*UseCaseTest` (found/not found)
- [ ] 6.2 `@QuarkusTest` catálogo Curso: SPEC-3.1–3.4, SPEC-4.1–4.3, 401 sem token (SPEC-4.8)
- [ ] 6.3 `@QuarkusTest` catálogo Aula: SPEC-3.5–3.9, SPEC-4.4–4.7 (`nome` no JSON, sem `titulo`), 401 regressão
- [ ] 6.4 Adaptar testes de auth/role que inspecionam body dos stubs antigos (`titulo`) para o novo contrato, sem mudar asserts de `AUTH_*`

## 7. Postman, cobertura e critério de saída

- [ ] 7.1 Atualizar `docs/postman/feedbacks-api.postman_collection.json`: body Aula com `nome`; incluir `GET /api/v1/cursos/{{cursoId}}`; capturar `cursoId`/`aulaId` nos 201 (UJ-2)
- [ ] 7.2 Rodar `mvn verify` — suíte verde; JaCoCo ≥ 90% no código de produção deste módulo (AD-14)
- [ ] 7.3 Conferir checkboxes do proposal.md §6 do módulo 02; auth (login, claims, `AUTH_*`) inalterado
