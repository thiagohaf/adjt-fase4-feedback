# Proposal — Módulo 01: Autenticação e Papéis

| Campo | Valor |
| --- | --- |
| **Módulo** | `01-autenticacao-e-papeis` |
| **PRD** | §4.1 Autenticação e papéis |
| **FRs vinculados** | FR-1, FR-2 |
| **User Journeys** | UJ-1 (Ana — Estudante), UJ-2 (Bruno — Administrador) |
| **Status** | Rascunho para revisão |
| **Dependências** | Nenhuma (módulo fundacional) |
| **Bloqueia** | Catálogo, Inscrição, Avaliação, listagens |

---

## 1. Problema

A Plataforma de Feedbacks FIAP expõe uma API REST sem interface web. Toda interação ocorre via Postman com credenciais distintas para **Estudante** e **Administrador**. Sem autenticação e autorização server-side:

- Não há como garantir que apenas Estudantes inscritos criem Avaliações.
- Não há como restringir criação de Curso/Aula ao Administrador.
- Não há como isolar a listagem de Avaliações (Admin vê todas; Estudante vê só as próprias).
- A demonstração acadêmica (vídeo YouTube, cena 4 — Auth) não pode ser executada.

O enunciado e o PRD exigem JWT completo: emissão no login, validação em rotas protegidas e distinção explícita de papéis.

## 2. Valor de negócio

| Stakeholder | Valor entregue |
| --- | --- |
| **Estudante (Ana)** | Acesso seguro para inscrever-se e avaliar Aulas após login único com token reutilizável na sessão Postman. |
| **Administrador (Bruno)** | Acesso privilegiado para montar catálogo e consultar Avaliações sem risco de ação de Estudante. |
| **Desenvolvedor (Thiago)** | Base de segurança demonstrável no vídeo (login Admin + login Estudante); conformidade com NFR de segurança (JWT obrigatório, segredos fora do código, papéis enforced server-side). |
| **Produto** | Habilita UJ-1 e UJ-2; desbloqueia todos os módulos subsequentes da API. |

**Métricas de sucesso relacionadas:**

- **SM-1:** Cobertura do enunciado (API autenticada).
- **SM-5:** Fluxo UJ-1/UJ-2 reproduzível em Postman em < 10 min (seed + tokens).
- **Roteiro §11, cena 4:** Login Admin + login Estudante com tokens visíveis.

## 3. Escopo

### 3.1 In scope (MVP)

| # | Capacidade | Detalhe |
| --- | --- | --- |
| 1 | **Login e emissão de JWT** | `POST /api/v1/auth/login` público; credenciais válidas retornam JWT utilizável. |
| 2 | **Validação de token** | Rotas de negócio exigem `Authorization: Bearer <JWT>`; token inválido/expirado → acesso negado. |
| 3 | **Autorização por papel** | Claim `role` ∈ {`ESTUDANTE`, `ADMINISTRADOR`}; matriz de rotas enforced server-side (Quarkus Security). |
| 4 | **Persistência de usuários** | Entidade `Usuario` com email, senha (hash BCrypt) e papel; seed Flyway para demo. |
| 5 | **Segredos** | `jwtSecret` em variável de ambiente local / Secrets Manager em AWS (AD-12). |
| 6 | **Erros padronizados** | Corpo `{ code, message, traceId }`; 401 credenciais/token; 403 papel insuficiente. |

### 3.2 Out of scope (MVP)

| Item | Motivo |
| --- | --- |
| Refresh token / rotação de JWT | PRD §6.2 — fora do MVP |
| SSO / federação de identidade | Non-goal §5 |
| Registro de usuário (signup) | Usuários seed/demo; um Admin único |
| Autenticação nas Lambdas | AD-8 — Lambdas não autenticam usuários finais |
| Multi-tenant / múltiplos Administradores | Assumption PRD — um Admin de demo |
| MFA, rate limiting avançado, CAPTCHA | Complexidade desnecessária para demo acadêmica |
| Recuperação de senha | Fora do escopo acadêmico |

### 3.3 Matriz de autorização (referência)

Derivada de AD-8; detalhada em `specs.md` e `design.md`.

| Recurso / Ação | ESTUDANTE | ADMINISTRADOR |
| --- | --- | --- |
| Login | ✓ (público) | ✓ (público) |
| Health check | ✓ (público) | ✓ (público) |
| Criar Curso/Aula | ✗ | ✓ |
| Listar/consultar Curso/Aula | ✓ | ✓ |
| Inscrever-se em Curso/Aula | ✓ | ✗ |
| Criar Avaliação | ✓ | ✗ |
| Listar Avaliações | ✓ (próprias) | ✓ (todas) |

### 3.4 Premissas e decisões arquiteturais aplicáveis

- JWT **HS256**; claim `role` explícito (AD-8).
- Prefixo HTTP `/api/v1/`; login e health públicos (AD-9).
- Segredo JWT **nunca** no código-fonte ou imagem Docker (AD-12, NFR §8).
- Expiração JWT configurável (sugestão: 24h para demo Postman).
- Usuários de demo criados via migration Flyway (`V2__seed_usuarios.sql`; `V1` cria o schema).

## 4. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| Autorização só no Postman (client-side) | `@RolesAllowed` server-side |
| Secret JWT vazando no repo | Secrets Manager + `.gitignore`; validação no CI |
| Confusão de papéis na demo | Seed com emails distintos (`estudante@demo.fiap`, `admin@demo.fiap`) documentados no Postman collection |
| Token expirado durante gravação do vídeo | TTL generoso (24h) + re-login rápido no roteiro |

## 5. Entregáveis deste módulo

1. Endpoint de login funcional com JWT.
2. Validação de token em rotas protegidas (extensão SmallRye JWT + `@RolesAllowed`).
3. Entidade `Usuario` + repositório + seed.
4. Testes unitários (casos de uso + token service) com cobertura JaCoCo ≥ 90% (AD-14).
5. Documentação OpenSpec (`proposal.md`, `specs.md`, `design.md`) — este pacote.

## 6. Critério de pronto do módulo

- [ ] Login Estudante e Admin retornam JWT com claim `role` correto.
- [ ] Rotas protegidas rejeitam request sem token (401).
- [ ] Rotas protegidas rejeitam token expirado/inválido (401).
- [ ] Estudante recebe 403 ao tentar criar Curso.
- [ ] Administrador recebe 403 ao tentar criar Avaliação (quando endpoint existir).
- [ ] Health e login permanecem acessíveis sem token.
- [ ] Cena 4 do roteiro YouTube executável com a collection Postman.
