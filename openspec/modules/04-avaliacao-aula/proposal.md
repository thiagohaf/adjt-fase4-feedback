# Proposal — Módulo 04: Avaliação de Aula

| Campo | Valor |
| --- | --- |
| **Módulo** | `04-avaliacao-aula` |
| **PRD** | §4.4 Avaliação de Aula; §4.5 Urgência (classificação) |
| **FRs vinculados** | FR-7, FR-8, FR-9 (+ preparação AD-4/AD-5 para FR-10) |
| **User Journeys** | UJ-1 (Ana — Estudante); UJ-2 listagem (Bruno) |
| **Status** | Rascunho para revisão |
| **Dependências** | Módulo 01 (auth); 02 (Curso/Aula); 03 (inscrição + gate FR-6) |
| **Bloqueia** | Alerta SES (FR-10), Relatórios (FR-11+) — precisam de Avaliação real |

---

## 1. Problema

Com inscrição entregue, Ana ainda encontra stub em `POST /api/v1/avaliacoes`: 201 com UUID
aleatório, sem `nota`, sem Urgência, sem unicidade Estudante+Aula. A listagem só devolve
`id`/`estudanteId`/`descricao` do schema V1.

Sem domínio real de Avaliação:

- UJ-1 (avaliar Aula) não é demonstrável de ponta a ponta.
- FR-7–FR-9 ficam sem cobertura; AD-16 (read model relatório) sem colunas.
- FR-10 (alerta ALTA) não tem evento para consumir.

## 2. Valor de negócio

| Stakeholder | Valor entregue |
| --- | --- |
| **Estudante (Ana)** | Envia feedback com nota; vê confirmação com Urgência; não reavalia a mesma Aula. |
| **Administrador (Bruno)** | Lista Avaliações com nota/Urgência/data/vínculos no Postman. |
| **Desenvolvedor** | Hexágono + V5; porta de publish pronta para Lambda de alerta. |
| **Produto** | Fecha UJ-1; desbloqueia alerta e relatórios. |

**Métricas / roteiro:** SM-1 (enunciado), cena avaliação + fronteira nota ≤4.

## 3. Escopo

### 3.1 In scope (MVP)

| # | Capacidade | Detalhe |
| --- | --- | --- |
| 1 | **Criar Avaliação** | `POST /api/v1/avaliacoes` — Estudante; `aulaId` + `descricao` + `nota` 0–10; gate FR-6. |
| 2 | **Urgência** | ALTA ≤4; MÉDIA 5–7; BAIXA ≥8; fronteiras 4/5/7/8. |
| 3 | **Unicidade** | `(estudanteId, aulaId)` → 409 `AVALIACAO_DUPLICADA`; sem upsert. |
| 4 | **Persistência** | Evoluir `avaliacao` via Flyway **V5**; não reescrever V1–V4. |
| 5 | **Listagem** | `GET /api/v1/avaliacoes` enriquecida; Admin todas / Estudante próprias. |
| 6 | **Publish ALTA** | Porta `EvaluationEventPublisher` + adapters; falha não reverte Avaliação. |
| 7 | **Erros** | Envelope `{ code, message, traceId }`; reutilizar `AULA_NOT_FOUND` / gate inscrição. |
| 8 | **Postman** | UJ-1 até Avaliação real; nota de fronteira ALTA. |

### 3.2 Out of scope (MVP)

| Item | Motivo |
| --- | --- |
| Lambda SES / e-mail alerta (FR-10) | Módulo seguinte; só publish |
| Relatórios diário/semanal (FR-11+) | Dependem de AD-16 + Lambda report |
| Update / delete / upsert Avaliação | PRD: sem upsert no MVP |
| Alterar auth / catálogo / inscrição | Contratos estáveis 01–03 |
| ECR / CDK deploy (FR-15 restante) | Fora deste módulo |

### 3.3 Matriz de autorização

| Recurso / Ação | ESTUDANTE | ADMINISTRADOR |
| --- | --- | --- |
| Criar Avaliação | ✓ (+ gate inscrição) | ✗ (`AUTH_FORBIDDEN`) |
| Listar Avaliações | ✓ (próprias) | ✓ (todas) |

### 3.4 Premissas

- Path `/api/v1/avaliacoes` e roles já existem — substituir stub, não reinventar path.
- `estudanteId` sempre do JWT; `aulaId`/`descricao`/`nota` no body.
- Hexagonal AD-2; JaCoCo ≥ 90% (AD-14); publish pós-commit (AD-4).

## 4. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| Testes do stub 201 / SPEC-6.2 | Atualizar para payload real + delta OpenSpec do gate |
| Schema V1 incompleto | V5 ADD COLUMN + UNIQUE; ambientes de teste Flyway limpo |
| Publish sem broker no CI | Fake in-memory no profile test |
| Confusão FR-9 vs FR-10 | Urgência no domínio; SES só no módulo alerta |

## 5. Entregáveis

1. Domínio `Avaliacao` / `Urgencia` + use cases + adapters JPA + publish.
2. Resource/DTOs reais; ExceptionMapper duplicata.
3. Migration Flyway V5.
4. Testes (unit + `@QuarkusTest`); JaCoCo ≥ 90%.
5. Collection Postman atualizada.
6. Documentação OpenSpec deste pacote + change `avaliacao-aula-quarkus`.

## 6. Critério de pronto do módulo

- [ ] Estudante inscrito cria Avaliação válida → 201 com Urgência e ids estáveis.
- [ ] `nota` fora 0–10 ou `descricao` inválida → 400 `VALIDATION_ERROR`.
- [ ] Sem inscrição na Aula → 403 `INSCRICAO_AULA_OBRIGATORIA` (FR-6).
- [ ] Duplicata Estudante+Aula → 409 `AVALIACAO_DUPLICADA`.
- [ ] Fronteiras 4/5/7/8 classificam ALTA/MEDIA/BAIXA corretamente.
- [ ] Admin lista todas com nota/Urgência/data/vínculos; Estudante só as próprias.
- [ ] ALTA publica evento AD-5; MÉDIA/BAIXA não; falha de publish não apaga Avaliação.
- [ ] Auth, catálogo e inscrição inalterados nos contratos públicos.
- [ ] Fluxo UJ-1 reproduzível na collection Postman até Avaliação real.
