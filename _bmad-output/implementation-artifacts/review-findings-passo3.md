# Review Findings — Passo 3 (Re-baseline Spring → Quarkus, módulo 01)

| Campo | Valor |
| --- | --- |
| **Data** | 2026-07-21 |
| **Escopo** | Mudanças não commitadas em `openspec/modules/01-autenticacao-e-papeis/design.md` e `proposal.md` |
| **Método** | Adversarial review (`bmad-review-adversarial-general`) sobre `git diff`, arquivos completos, `specs.md` (inalterado), Architecture Spine e Sprint Change Proposal 2026-07-21 (Passo 3); afirmações de configuração SmallRye/Quarkus verificadas contra a documentação oficial do Quarkus (guia "Using JWT RBAC") e SmallRye JWT |
| **Resultado** | 16 findings: 4 HIGH, 5 MEDIUM, 6 LOW, 1 INFO/positivo |

---

## Findings

### 1. HIGH — `openspec/modules/01-autenticacao-e-papeis/design.md:335` (repetido na linha 359) — `smallrye.jwt.claims.groups=role` faz o oposto do que o documento afirma

**Descrição:** O doc afirma que a propriedade mapeia "claim `role` → roles do SecurityIdentity". Pela documentação SmallRye/Quarkus JWT, `smallrye.jwt.claims.groups` define um valor *default* de groups usado apenas quando o token **não** possui claim de groups. Configurado como está, todo usuário autenticado receberia o group literal `"role"` e a claim `role` real seria ignorada — quebrando toda a matriz de autorização (todos os checks `@RolesAllowed` retornariam 403). A propriedade correta para remapear uma claim custom para roles é `smallrye.jwt.path.groups=role`.

**Evidência:** §7.1 linha 335 (`smallrye.jwt.claims.groups=role               # claim \`role\` → roles do SecurityIdentity`) e §7.3 linha 359 repetem a mesma propriedade errada em prosa, então é decisão de design, não typo. Docs Quarkus/SmallRye: "`smallrye.jwt.claims.groups` — This property can set a default groups claim value when the current token has no standard or custom groups claim available"; "`smallrye.jwt.path.groups` — Path to the claim containing the groups... can be used if a token has no 'groups' claim but has the groups set in a different claim."

---

### 2. HIGH — `design.md:334` — `smallrye.jwt.verify.key` não é uma propriedade de configuração real

**Descrição:** §7.1 contém `smallrye.jwt.verify.key=${JWT_SECRET}`. As propriedades documentadas são `smallrye.jwt.verify.secretkey` (inline) ou `smallrye.jwt.verify.key.location` (arquivo/URI); para chaves simétricas o valor deve estar em formato JWK (`{"kty":"oct","k":"..."}`, base64url-encoded quando inline), não uma string de secret crua. Como está, a validação de token falharia no startup ou nunca resolveria a chave.

**Evidência:** Docs SmallRye: "`smallrye.jwt.verify.secretkey` — Secret key supplied as a string"; "`smallrye.jwt.verify.key.location` — ... Secret keys can only be in the JWK format". Guia Quarkus: "If you need to verify the token signature by using the symmetric secret key, then either a JSON Web Key (JWK) or JSON Web Key Set format must be used". A abordagem env-var-crua-como-secret precisa de decisão de design explícita (wrapping/encoding JWK do valor vindo do Secrets Manager).

---

### 3. HIGH — `design.md:333` vs `design.md:309` e `design.md:316-322` — verificação de issuer contradiz o próprio contrato de claims do módulo

**Descrição:** §7.1 introduz `mp.jwt.verify.issuer=feedbacks-api`, mas §6 define as claims como exatamente `sub`, `role`, `iat`, `exp`, e o payload de exemplo decodificado não contém `iss`. As responsabilidades de `TokenService.generate` (linha 128) também não mencionam emitir `iss`. Se o emissor seguir §6 e o validador seguir §7.1, **todo token é rejeitado**.

**Evidência:** Linha 333 (`mp.jwt.verify.issuer=feedbacks-api`); linha 309 (`Claims | \`sub\` (UUID string), \`role\` (string), \`iat\`, \`exp\``); payload exemplo linhas 316-322 sem `iss`. Ou `iss` entra no contrato de claims (o que toca um invariante congelado — o conjunto de claims fazia parte do contrato que não deve mudar) ou a linha de issuer deve ser removida. O valor `feedbacks-api` não é rastreável a nenhum AD, spec ou ao Spine.

---

### 4. HIGH — `design.md:412-419` (e `382-385`) — o envelope de erro `AUTH_MISSING_TOKEN` / `AUTH_INVALID_TOKEN` / `AUTH_TOKEN_EXPIRED` depende de pré-requisito Quarkus não declarado

**Descrição:** §8.3 assume que `ExceptionMapper<UnauthorizedException>` e `ExceptionMapper<AuthenticationFailedException>` produzirão o corpo `{code, message, traceId}`. No Quarkus, a autenticação proativa (default) executa **antes** do roteamento JAX-RS, então esses mappers nunca são invocados a menos que `quarkus.http.auth.proactive=false` seja configurado (ou um `HttpAuthenticationMechanism` custom seja usado). O design não menciona nenhum dos dois. Como esses códigos de erro são invariantes congelados de contrato testados por SPEC-1.8/1.9/1.10 em `specs.md`, a omissão coloca o contrato em risco de implementação.

**Evidência:** Linhas 412-419 (mappers para `UnauthorizedException` e `AuthenticationFailedException`) e 382-385 (hierarquia Quarkus Security) sem menção a `quarkus.http.auth.proactive`. A nota "distinguir expiração inspecionando a causa" (linha 418) também é vaga — o design deveria declarar qual causa/exceção distingue expiração, já que `AUTH_TOKEN_EXPIRED` vs `AUTH_INVALID_TOKEN` é distinção de nível de spec.

---

### 5. MEDIUM — `design.md:347-352` — a listagem de rotas de autorização estreita silenciosamente a matriz wildcard antiga

**Descrição:** A config Spring removida protegia `POST /api/v1/**/aulas/**` e `POST /api/v1/**/inscricoes/**` (qualquer path de inscrição, incluindo nível de aula). A nova listagem fixa apenas `POST /api/v1/cursos/{id}/aulas` e `POST /api/v1/cursos/{id}/inscricoes`. Uma rota de inscrição em nível de aula cairia no fallback `@Authenticated` (comentário da linha 352), o que permitiria a um ADMINISTRADOR se inscrever — violando a matriz, que foi declarada invariante.

**Evidência:** `specs.md` SPEC-2.8 diz explicitamente "`POST /api/v1/cursos/{id}/inscricoes` **ou equivalente de inscrição**"; matriz do proposal.md linha 76 cobre "Inscrever-se em Curso/Aula" (Curso **e** Aula). O estreitamento precisa ser justificado ou a listagem generalizada.

---

### 6. MEDIUM — `design.md:332` e `design.md:446` — `mp.jwt.verify.publickey.algorithm=HS256` é duvidoso segundo docs oficiais (herdado do Spine)

**Descrição:** O guia Quarkus JWT RBAC declara que para chaves simétricas "`smallrye.jwt.verify.algorithm` should be set to `HS256`/`HS384`/`HS512`", e documenta `mp.jwt.verify.publickey.algorithm` para algoritmos assimétricos (`RS256`/`ES256`); a spec MicroProfile JWT não suporta algoritmos simétricos (extensão específica do SmallRye). O design espelha fielmente o AD-8 do Architecture Spine, então é *coerente* com o baseline — mas o próprio baseline codifica um par propriedade/valor que os docs oficiais não endossam.

**Evidência:** Guia Quarkus: "`mp.jwt.verify.publickey.algorithm` | `RS256` | List of signature algorithms. Set it to `ES256`..."; "`smallrye.jwt.verify.algorithm` — This property should only be used to set a symmetric algorithm such as `HS256`". Se falhar na implementação, a redação literal do invariante ("via `mp.jwt.verify.publickey.algorithm=HS256`") terá que mudar.

---

### 7. MEDIUM — `design.md:35`, `design.md:270-275`, `design.md:515-529` — BCrypt perdeu seu provider no re-baseline

**Descrição:** O diagrama e §5.2 ainda dependem de `PasswordEncoder BCrypt`, mas o stack Spring que fornecia a implementação BCrypt (`spring-boot-starter-security`) foi removido, e a nova lista de dependências Maven em §12 não contém nenhuma fonte de BCrypt (ex.: `quarkus-elytron-security-common` com `BcryptUtil`, ou biblioteca bcrypt standalone). O design agora especifica uma capacidade sem implementação declarada.

**Evidência:** Linha 35 (`PC[PasswordEncoder BCrypt]` no diagrama); linhas 270-275 (`senhaCorresponde(String raw, PasswordEncoder encoder)`); §12 linhas 515-529 (lista de deps sem bcrypt). A migration de seed (linha 298, "Hash BCrypt gerado no migration") e SPEC-NFR-3 dependem disso.

---

### 8. MEDIUM — `design.md:334` vs `design.md:444-455` — duas configurações de secret independentes sem fonte única de verdade; chave de validação sem default local

**Descrição:** §7.1 liga a validação a `smallrye.jwt.verify.key=${JWT_SECRET}` (sem profile, sem default), enquanto §9.2 liga a emissão a `%local.app.jwt.secret=${JWT_SECRET:dev-only-secret-min-256-bits-for-hs256-demo}` (com default de dev). Localmente, sem `JWT_SECRET` exportado, a emissão usaria o secret de fallback enquanto a validação não teria nenhum — tokens auto-emitidos falhariam na verificação. Nada declara que ambas as propriedades devem resolver para o mesmo valor.

**Evidência:** Linha 334 (validação, global, sem default) vs linhas 451-455 (emissão, com default `%local` e `%aws`).

---

### 9. MEDIUM — `openspec/modules/01-autenticacao-e-papeis/proposal.md:86` vs `design.md:291` — contradição V1/V2 na migration de seed (pré-existente, não capturada pelo passo de re-baseline)

**Descrição:** proposal.md §3.4 diz "Usuários de demo criados via migration Flyway (`V1__seed_usuarios.sql`)"; design.md §5.3 intitula o seed `V2__seed_usuarios.sql` (V1 presumivelmente sendo o schema). O invariante Flyway V1/V2 está internamente ambíguo entre os dois arquivos.

**Evidência:** proposal.md linha 86 vs design.md linha 291. A Sprint Change Proposal Passo 4 confirma "migrações Flyway V1/V2". O passo de edição tocou proposal.md duas linhas adiante (linha 92 no diff, mudança de mitigação de risco) e deixou essa inconsistência de pé.

---

### 10. LOW — `design.md:385` — atribuição de package errada para `ForbiddenException`

**Descrição:** §8.1 lista "`ForbiddenException` (jakarta) → 403 AUTH_FORBIDDEN". A negação de `@RolesAllowed` do Quarkus Security lança `io.quarkus.security.ForbiddenException`, não a `jakarta.ws.rs.ForbiddenException` do JAX-RS. Mapear a jakarta perderia as negações RBAC reais.

**Evidência:** Linha 385. Quebraria SPEC-2.1/2.2/2.6 (403 `AUTH_FORBIDDEN` por papel insuficiente).

---

### 11. LOW — `proposal.md:100` — redação residual da era Spring

**Descrição:** O entregável 2 ainda diz "Filter/Security chain validando token em rotas protegidas" — "Security chain" é vocabulário do Spring (`SecurityFilterChain`); no design Quarkus não há filter chain, a extensão valida.

**Evidência:** proposal.md linha 100. Contra o critério de sucesso declarado ("no residual Spring references except historical record"), é um resquício.

---

### 12. LOW — `design.md:508` — `<version>3.33.x</version>` não é uma versão Maven resolvível

**Descrição:** O placeholder `3.33.x` no import do `quarkus-bom` não builda se copiado e colado; um pom de referência deveria fixar versão concreta ou marcar explicitamente como placeholder (`${quarkus.platform.version}`).

**Evidência:** Linha 508 (`<version>3.33.x</version> <!-- LTS -->`).

---

### 13. LOW — `design.md:19-21`, `design.md:38` — diagrama Mermaid com nós soltos e implicação invertida

**Descrição:** `TF[TraceIdFilter ContainerRequestFilter]` e `EH[ExceptionMappers]` são declarados sem nenhuma aresta (o diagrama antigo ao menos conectava `JF --> TS`), e `QS[Quarkus Security / SmallRye JWT] --> SI` se lê como o framework dependendo do adapter, quando o adapter lê *do* `SecurityIdentity`. Além disso, `SI --> CP` retrata infrastructure dependendo de porta de application — consistente com a árvore de pacotes, mas o diagrama não mostra mais como as requests chegam a `TF`/`EH`.

**Evidência:** Linhas 19-21 (declaração de TF/EH), linha 38 (`QS --> SI`), linha 45 (`SI --> CP`).

---

### 14. LOW — `design.md:536-540` — decisão "resolvida" deixada dentro da tabela "Decisões em aberto"

**Descrição:** O item 1 de §13 está marcado "**Resolvida** (re-baseline Quarkus 2026-07-21)" mas permanece em tabela intitulada "Decisões em aberto (para revisão)". Decisões resolvidas pertencem a um registro de decisões, não à lista de questões abertas.

**Evidência:** Linhas 536-540 (tabela §13, item 1).

---

### 15. LOW — `design.md:344-345` vs `specs.md:124` — o path de contrato `/api/v1/health` não tem mecanismo de exposição declarado

**Descrição:** SPEC-1.11 (inalterado, e corretamente) exige que `GET /api/v1/health` responda sem auth; o design agora diz que health é SmallRye Health em `/q/health` "exposto em path compatível com o contrato", adiando o "como" para o módulo de observabilidade. O design antigo ao menos era dono da rota pública no `SecurityConfig`. O adiamento é reconhecido em §14, mas o mecanismo real de mapeamento (`quarkus.smallrye-health.root-path` vs resource fino delegando ao health check) é uma decisão sem dono atual.

**Evidência:** design.md linhas 344-345 e 550; specs.md linha 124 (SPEC-1.11 cita `GET /api/v1/health`); AD-11 do Spine ("exposto em path compatível com o ALB health check").

---

### 16. INFO / POSITIVO — módulo inteiro — sem referências Spring residuais; invariantes congelados preservados no papel

**Descrição:** Grep case-insensitive por "spring" em `openspec/modules/01-autenticacao-e-papeis/` retorna zero matches. O mapa de códigos de erro (`AUTH_INVALID_CREDENTIALS`, `AUTH_MISSING_TOKEN`, `AUTH_INVALID_TOKEN`, `AUTH_TOKEN_EXPIRED`, `AUTH_FORBIDDEN`, `VALIDATION_ERROR`), o envelope `{code, message, traceId}`, os papéis `ESTUDANTE`/`ADMINISTRADOR`, HS256, `jwtSecret` no Secrets Manager e os contratos `/api/v1/` batem com `specs.md` (intocado, corretamente — é agnóstico a mecanismo) e com AD-8/AD-9/AD-12 do Spine.

**Evidência:** Grep sem matches; specs.md inalterado no diff; a mudança de `GET /api/v1/avaliacoes` de `.authenticated()` para `@RolesAllowed({"ESTUDANTE","ADMINISTRADOR"})` é comportamentalmente equivalente dado que só existem dois papéis.

---

## Veredicto

As mudanças em `proposal.md` são mínimas e corretas. A reescrita de `design.md` preserva os invariantes congelados *no papel*, mas os findings 1–4 quebrariam autenticação/autorização por completo se implementados literalmente (propriedade de groups mal utilizada, propriedade de chave de verificação inexistente, issuer verificado mas nunca emitido, e mappers de exceção de auth que não disparam sob autenticação proativa default). Esses pontos devem ser corrigidos em `design.md` antes do início do Passo 4 (implementação).
