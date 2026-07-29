# Proposal — Módulo 03: Inscrição Curso/Aula

| Campo | Valor |
| --- | --- |
| **Módulo** | `03-inscricao-curso-aula` |
| **PRD** | §4.3 Inscrição do Estudante |
| **FRs vinculados** | FR-5, FR-6 |
| **User Journeys** | UJ-1 (Ana — Estudante) |
| **Status** | Rascunho para revisão |
| **Dependências** | Módulo 01 (auth JWT + `@RolesAllowed`); Módulo 02 (Curso/Aula persistidos) |
| **Bloqueia** | Avaliação (FR-7+) — gate FR-6 |

---

## 1. Problema

Com catálogo entregue, Ana (Estudante) ainda só encontra o stub
`POST /api/v1/cursos/{id}/inscricoes`, que devolve `INSCRITO` sem persistir vínculo.
Não há inscrição em Aula, nem regra de duplicata, nem gate que impeça Avaliar sem inscrição.

Sem domínio real de inscrição:

- UJ-1 (cena inscrição + avaliação) não é demonstrável de ponta a ponta.
- FR-5 (Curso + Aula; rejeitar duplicata; Aula só se inscrito no Curso) fica sem cobertura.
- FR-6 (Avaliação exige inscrição na Aula) não tem porta de consulta nem rejeição no stub de `POST /avaliacao`.
- Módulo Avaliação nasce sem base AD-15 (`INSCRICAO_CURSO` / `INSCRICAO_AULA`).

## 2. Valor de negócio

| Stakeholder | Valor entregue |
| --- | --- |
| **Estudante (Ana)** | Inscreve-se em Curso e depois na Aula; duplicata e pré-requisito falham com erro claro. |
| **Administrador** | Continua sem se inscrever; papéis `AUTH_*` intactos. |
| **Desenvolvedor** | Substitui stub por hexágono + Flyway V4; porta de verificação pronta para Avaliação. |
| **Produto** | Cena 6 do roteiro (inscrição) + pré-condição da Avaliação; desbloqueia FR-7. |

**Métricas / roteiro:** SM-1 (enunciado), UJ-1 no Postman, cena inscrição antes de avaliar.

## 3. Escopo

### 3.1 In scope (MVP)

| # | Capacidade | Detalhe |
| --- | --- | --- |
| 1 | **Inscrever em Curso** | `POST /api/v1/cursos/{cursoId}/inscricoes` — Estudante; Curso deve existir; `estudanteId` = `sub` do JWT. |
| 2 | **Inscrever em Aula** | `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` — Estudante; Aula do Curso; exige inscrição prévia no Curso. |
| 3 | **Duplicata → 409** | Par `(estudanteId, cursoId)` e `(estudanteId, aulaId)` únicos; código `INSCRICAO_DUPLICADA`. |
| 4 | **Persistência** | Tabelas `inscricao_curso` / `inscricao_aula` via Flyway **V4**; não reescrever V1–V3. |
| 5 | **Gate FR-6** | Porta de consulta “estudante inscrito na Aula?”; stub `POST /avaliacao` rejeita sem inscrição (sem implementar FR-7). |
| 6 | **Erros** | Envelope `{ code, message, traceId }`; reutilizar `CURSO_NOT_FOUND` / `AULA_NOT_FOUND`; auth inalterado. |
| 7 | **Postman** | Trocar stub de inscrição; adicionar inscrição em Aula; capturar ids no fluxo UJ-1. |

### 3.2 Out of scope (MVP)

| Item | Motivo |
| --- | --- |
| Avaliação completa (descricao/nota/Urgência) — FR-7+ | Módulo seguinte; só gate no stub |
| Listagem/consulta HTTP de inscrições | PRD não exige; query interna basta |
| Cancelar / update inscrição | MVP só cria |
| Admin se inscrever | Matriz: só ESTUDANTE |
| Alterar claims JWT / códigos `AUTH_*` / login | Contrato estável do módulo 01 |
| Alterar schema/contrato de Curso/Aula | Módulo 02 estável |
| Alertas, relatórios, seed de inscrição | Fora de §4.3 |

### 3.3 Matriz de autorização (inscrição)

Herdada do módulo 01 / AD-8 — **não reinventar**:

| Recurso / Ação | ESTUDANTE | ADMINISTRADOR |
| --- | --- | --- |
| Inscrever-se em Curso / Aula | ✓ | ✗ (403 `AUTH_FORBIDDEN`) |
| Criar Avaliação (stub + gate FR-6) | ✓ (papel); gate domínio à parte | ✗ (403) |
| Listar / consultar catálogo | ✓ | ✓ (sem inscrição) |

### 3.4 Premissas e decisões aplicáveis

- Path de inscrição em Curso já existe como stub; manter path/`@RolesAllowed("ESTUDANTE")` e substituir corpo por domínio real.
- Nova rota de inscrição em Aula sob o Curso (explícita no path) — alinha com “equivalente” de SPEC-2.8/2.9.
- Duplicata = **409** (não idempotente) — PRD ASSUMPTION + AD-15 + review adversarial (`INSCRICAO_DUPLICADA`).
- Inscrição em Aula exige inscrição no Curso **e** Aula pertencente ao `cursoId` do path.
- `estudanteId` sempre do JWT (`CurrentUserProvider`); nunca do body.
- Hexagonal: `api` → `application` → `domain` ← `infrastructure` (AD-2).
- JaCoCo ≥ 90% no código deste módulo (AD-14).

## 4. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| Auth tests que esperam 201 no stub sem Curso real | Adaptar fixtures: criar Curso (e inscrição Curso antes de Aula) nos testes de integração |
| Race de duplicata sob carga | UNIQUE no DB + mapear constraint violation → 409 |
| FR-6 confundido com FR-7 | Gate só rejeita sem inscrição; stub ainda não persiste Avaliação real |
| Path de Aula fora de `@RolesAllowed` | Declarar `@RolesAllowed("ESTUDANTE")` na rota nova (módulo 01) |
| Confusão Curso vs Aula inexistente | Reusar `CURSO_NOT_FOUND` / `AULA_NOT_FOUND`; Aula de outro Curso → `AULA_NOT_FOUND` |

## 5. Entregáveis deste módulo

1. Domínio `InscricaoCurso` / `InscricaoAula` + use cases + adapters JPA.
2. Resources: substituir stub de inscrição em Curso; adicionar inscrição em Aula.
3. Porta de verificação de inscrição + rejeição no stub `POST /avaliacao` (FR-6).
4. Migration Flyway V4.
5. Testes (unit + `@QuarkusTest`) cobrindo specs; JaCoCo ≥ 90%.
6. Collection Postman atualizada (UJ-1 inscrição Curso → Aula).
7. Documentação OpenSpec deste pacote (`proposal.md`, `specs.md`, `design.md`).

## 6. Critério de pronto do módulo

- [x] Estudante se inscreve em Curso existente → 201 com ids estáveis.
- [x] Estudante se inscreve em Aula do Curso após inscrição no Curso → 201.
- [x] Duplicata Curso ou Aula → 409 `INSCRICAO_DUPLICADA`.
- [x] Inscrição em Aula sem inscrição no Curso → rejeitada (código de domínio documentado).
- [x] Curso/Aula inexistentes → 404 `CURSO_NOT_FOUND` / `AULA_NOT_FOUND`.
- [x] Admin recebe 403 `AUTH_FORBIDDEN` ao tentar se inscrever.
- [x] `POST /avaliacao` sem inscrição na Aula → rejeitado (FR-6); com inscrição → stub não retorna 403 de domínio.
- [x] Auth (login, claims, `AUTH_*`) e catálogo (FR-3/4) inalterados.
- [x] Fluxo UJ-1 (inscrição) reproduzível na collection Postman.
