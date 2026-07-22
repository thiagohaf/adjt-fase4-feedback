# Proposal — Módulo 02: Catálogo Curso/Aula

| Campo | Valor |
| --- | --- |
| **Módulo** | `02-catalogo-curso-aula` |
| **PRD** | §4.2 Catálogo: Curso e Aula |
| **FRs vinculados** | FR-3, FR-4 |
| **User Journeys** | UJ-2 (Bruno — Administrador), UJ-1 (Ana — Estudante, leitura) |
| **Status** | Rascunho para revisão |
| **Dependências** | Módulo 01 — Autenticação e Papéis (JWT + `@RolesAllowed` estáveis) |
| **Bloqueia** | Inscrição (FR-5/6), Avaliação (FR-7+) |

---

## 1. Problema

Com o módulo 01 entregue, a API autentica e autoriza papéis, mas o catálogo ainda é **stub**: `CursoResource` / `AulaResource` devolvem dados inventados em memória e a Aula usa o campo `titulo`, divergente do PRD (`nome`).

Sem domínio e persistência reais:

- Bruno (Admin) não consegue montar o catálogo demonstrável no Postman / vídeo (UJ-2).
- Ana (Estudante) não tem Cursos/Aulas estáveis para se inscrever depois (UJ-1).
- Módulos de Inscrição e Avaliação ficam bloqueados (dependem de `cursoId` / `aulaId` persistidos).

## 2. Valor de negócio

| Stakeholder | Valor entregue |
| --- | --- |
| **Administrador (Bruno)** | Cria Curso e Aulas com nome/descrição; consulta o que criou. |
| **Estudante (Ana)** | Lista e consulta o catálogo antes de se inscrever. |
| **Desenvolvedor** | Substitui stubs por hexágono + Flyway; paths/`@RolesAllowed` do módulo 01 preservados. |
| **Produto** | Cena do catálogo no roteiro YouTube; desbloqueia Inscrição. |

**Métricas / roteiro:** SM-1 (cobertura do enunciado), UJ-2 no Postman, cena de catálogo Admin.

## 3. Escopo

### 3.1 In scope (MVP)

| # | Capacidade | Detalhe |
| --- | --- | --- |
| 1 | **Criar Curso** | `POST /api/v1/cursos` — Admin; `nome` obrigatório; `descricao` opcional. |
| 2 | **Criar Aula** | `POST /api/v1/cursos/{cursoId}/aulas` — Admin; `nome` obrigatório; `descricao` opcional; Curso deve existir. |
| 3 | **Listar / consultar Curso** | `GET /api/v1/cursos`, `GET /api/v1/cursos/{id}` — Estudante e Admin. |
| 4 | **Listar / consultar Aula** | `GET /api/v1/cursos/{cursoId}/aulas`, `GET /api/v1/aulas/{id}` — Estudante e Admin. |
| 5 | **Persistência** | Tabelas `curso` / `aula` via Flyway **nova** migration (`V3+`); não reescrever V1/V2 de auth. |
| 6 | **Erros** | Envelope `{ code, message, traceId }`; 400 validação; 404 id inexistente; auth inalterado. |
| 7 | **Contrato HTTP** | Campo JSON **`nome`** (não `titulo`) em request/response de Aula; atualizar Postman. |

### 3.2 Out of scope (MVP)

| Item | Motivo |
| --- | --- |
| Inscrição em Curso/Aula (FR-5/6) | Módulo seguinte; stub de inscrição permanece intocado |
| Avaliação, alertas, relatórios | Fora de §4.2 |
| Alterar claims JWT / códigos `AUTH_*` / login | Contrato estável do módulo 01 |
| Update/Delete de Curso ou Aula | PRD MVP só cria + lista/consulta |
| Unicidade de nome de Curso/Aula | Não exigida no PRD |
| Reabrir troca Spring → Quarkus | Já resolvida |

### 3.3 Matriz de autorização (catálogo)

Herdada do módulo 01 / AD-8 — **não reinventar**:

| Recurso / Ação | ESTUDANTE | ADMINISTRADOR |
| --- | --- | --- |
| Criar Curso / Aula | ✗ (403) | ✓ |
| Listar / consultar Curso / Aula | ✓ | ✓ |
| Inscrição (stub / futuro) | ✓ | ✗ |

### 3.4 Premissas e decisões aplicáveis

- Paths e `@RolesAllowed` dos stubs são o contrato estável; corpo stub → domínio real.
- **Alinhamento `titulo` → `nome`:** stub e Postman usam `titulo` na Aula; PRD/Spine exigem `nome`. O módulo 02 **corrige** o contrato da Aula para `nome` (breaking apenas do stub provisório).
- Prefixo `/api/v1/`; camelCase JSON; IDs UUID (AD-9 + Consistency Conventions).
- Hexagonal: `api` → `application` → `domain` ← `infrastructure` (AD-2).
- JaCoCo ≥ 90% no código deste módulo (AD-14).

## 4. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| Quebrar testes de auth que batem em stubs | Manter paths/`@RolesAllowed`; adaptar só assertions de body se dependerem de `titulo` |
| Reescrever V1/V2 Flyway | Só `V3__create_catalogo.sql` (ou nome equivalente) |
| Esquecer `GET /cursos/{id}` | Stub não tinha; PRD exige consulta por id — incluir no design |
| Confusão `titulo` vs `nome` | Specs + Postman + DTOs só com `nome` |

## 5. Entregáveis deste módulo

1. Domínio `Curso` / `Aula` + casos de uso + adapters JPA.
2. Resources JAX-RS substituindo stubs (exceto inscrição stub).
3. Migration Flyway do catálogo.
4. Testes (unit + `@QuarkusTest`) cobrindo specs; JaCoCo ≥ 90%.
5. Collection Postman atualizada (`nome` na Aula).
6. Documentação OpenSpec deste pacote (`proposal.md`, `specs.md`, `design.md`).

## 6. Critério de pronto do módulo

- [ ] Admin cria Curso e Aula persistidos; resposta 201 com `id`, `nome`, `descricao`.
- [ ] Estudante e Admin listam/consultam; id inexistente → 404 com código de domínio.
- [ ] Estudante recebe 403 ao criar Curso/Aula (`AUTH_FORBIDDEN`).
- [ ] Payload sem `nome` → 400 `VALIDATION_ERROR`.
- [ ] Criar Aula com `cursoId` inexistente → 404 `CURSO_NOT_FOUND`.
- [ ] Campo JSON da Aula é `nome` (não `titulo`).
- [ ] Auth (login, claims, `AUTH_*`) inalterado.
- [ ] UJ-2 reproduzível na collection Postman.
