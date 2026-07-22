# Critério de pronto (proposal.md §6) — mapeamento

| Checkbox (módulo 01 proposal §6) | Evidência |
| --- | --- |
| Login Estudante e Admin retornam JWT com claim `role` correto | `AuthLoginResourceTest.spec1_1_loginEstudante`, `spec1_2_loginAdmin` |
| Rotas protegidas rejeitam request sem token (401) | `AuthTokenValidationTest.spec1_8_semToken` → `AUTH_MISSING_TOKEN` |
| Rotas protegidas rejeitam token expirado/inválido (401) | `spec1_9_tokenInvalido` → `AUTH_INVALID_TOKEN`; `spec1_10_tokenExpirado` → `AUTH_TOKEN_EXPIRED` |
| Estudante recebe 403 ao tentar criar Curso | `AuthRoleAuthorizationTest.spec2_1_estudanteNaoCriaCurso` → `AUTH_FORBIDDEN` |
| Administrador recebe 403 ao tentar criar Avaliação | `AuthRoleAuthorizationTest.spec2_6_adminNaoCriaAvaliacao` → `AUTH_FORBIDDEN` |
| Health e login permanecem acessíveis sem token | `AuthTokenValidationTest.spec1_11_rotasPublicas` |
| Cena 4 do roteiro YouTube executável com a collection Postman | Fora do escopo de código desta change (artefato Postman/demo); superfície HTTP pronta: `POST /api/v1/auth/login`, seeds `estudante@demo.fiap` / `admin@demo.fiap` |

**Suíte:** `mvn verify` — 50 testes, JaCoCo line ≥ 90%, `grep springframework` vazio.
