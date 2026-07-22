# Tasks — Implementar Módulo 04: Avaliação de Aula (Quarkus)

> Referências: specs desta change (`specs/avaliacao-*/spec.md`,
> `specs/inscricao-gate-avaliacao/spec.md`), design desta change (`design.md`, D1–D7) e o
> design do módulo `openspec/modules/04-avaliacao-aula/design.md`.

## 1. Branch e baseline

- [x] 1.1 Criar branch `feature/openspec-04-avaliacao-quarkus` a partir de `develop` atualizado
- [ ] 1.2 Confirmar stub atual (`AvaliacaoResource.criar`), gate FR-6, `ListarAvaliacoesUseCase`,
      `AvaliacaoEntity` V1 e Flyway V1–V4 intactos — baseline

## 2. Domínio e exceções

- [ ] 2.1 Criar `domain/avaliacao/{Avaliacao,Urgencia}.java` (factory com nota 0–10 → Urgência;
      `ocorridoEm`; vínculos `aulaId`/`cursoId`) conforme design do módulo
- [ ] 2.2 Criar `domain/exception/AvaliacaoDuplicadaException.java` (código `AVALIACAO_DUPLICADA`);
      reutilizar `AulaNotFoundException` / `InscricaoAulaObrigatoriaException`

## 3. Application — portas e use cases

- [ ] 3.1 Criar portas `application/avaliacao/port/{AvaliacaoRepository,EvaluationEventPublisher}.java`
      (save, existsByEstudanteAndAula, find*; publish payload AD-5)
- [ ] 3.2 Implementar `CriarAvaliacaoUseCase`: gate inscrição → resolve Aula → unicidade →
      persistir + Urgência → pós-commit publish se ALTA
- [ ] 3.3 Atualizar `ListarAvaliacoesUseCase` / `AvaliacaoReadRepository` para DTO enriquecido
      (nota, urgencia, ocorridoEm, aulaId, cursoId) mantendo filtro por papel

## 4. Infraestrutura — JPA + Flyway V5 + publish

- [ ] 4.1 Criar `V5__evolve_avaliacao.sql` (ADD colunas + UNIQUE + CHECK + índice);
      **não** alterar V1–V4; sem seed
- [ ] 4.2 Evoluir `AvaliacaoEntity` + adapter `JpaAvaliacaoRepository`; mapear UNIQUE →
      `AvaliacaoDuplicadaException`
- [ ] 4.3 Implementar adapters `EvaluationEventPublisher`: fake/in-memory (test), Kafka
      `%local` (e/ou no-op documentado se broker ausente), preparação SQS `%aws`

## 5. Camada web — resources, DTOs e erros

- [ ] 5.1 Substituir `CriarAvaliacaoStubRequest` por `CriarAvaliacaoRequest` (`aulaId`,
      `descricao` `@NotBlank`, `nota` `@Min(0)` `@Max(10)` `@NotNull`) + `AvaliacaoResponse`
- [ ] 5.2 Substituir stub em `AvaliacaoResource.criar` pelo use case real; manter
      `@RolesAllowed("ESTUDANTE")` e path `/api/v1/avaliacoes`
- [ ] 5.3 Registrar ExceptionMapper `AvaliacaoDuplicadaException` → 409 `AVALIACAO_DUPLICADA`;
      não alterar mapa `AUTH_*` / inscrição

## 6. Testes (cada SPEC → teste)

- [ ] 6.1 Unit: `UrgenciaTest` (fronteiras 4/5/7/8); `CriarAvaliacaoUseCaseTest`
      (sucesso, duplicata, sem inscrição, nota inválida, publish ALTA vs não-ALTA, falha publish)
- [ ] 6.2 `@QuarkusTest` criação: SPEC-7.1–7.7 + regressão SPEC-6.1 (403 sem inscrição)
- [ ] 6.3 `@QuarkusTest` Urgência + listagem: SPEC-9.x, SPEC-8.x; adaptar SPEC-2.7/2.10/2.11
      ao payload real sem mudar asserts `AUTH_*`
- [ ] 6.4 Testes de publish: SPEC-10prep.1–10prep.5 com fake publisher

## 7. Postman, cobertura e critério de saída

- [ ] 7.1 Atualizar `docs/postman/feedbacks-api.postman_collection.json`: body com `nota`;
      assert Urgência; UJ-1 até Avaliação persistida (incl. nota ≤4)
- [ ] 7.2 Rodar `mvn verify` — suíte verde; JaCoCo ≥ 90% no código de produção deste módulo (AD-14)
- [ ] 7.3 Conferir checkboxes do proposal.md §6 do módulo 04; auth/catálogo/inscrição inalterados
      nos contratos `AUTH_*` / Curso-Aula / inscrição
