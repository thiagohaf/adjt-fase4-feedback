# Design — Implementar Módulo 01: Autenticação e Papéis (Quarkus)

> O design técnico completo já existe em
> `openspec/modules/01-autenticacao-e-papeis/design.md` (estrutura de pacotes §2, portas §3,
> DTOs §4, domínio §5, contrato JWT §6, segurança SmallRye §7, ExceptionMappers §8, config §9,
> endpoint §10, testes §11, pom §12). **Este documento não o duplica** — registra apenas as
> decisões de execução desta change (bootstrap do projeto, reaproveitamento do branch Spring,
> stubs para a matriz de autorização, estratégia de teste/cobertura).

## Context

- Repositório sem código de aplicação: `feedbacks/apps/api/` não existe no `develop`
  (limpar resíduo untracked `target/` se aparecer). Trabalho no branch
  `feature/openspec-01-autenticacao-e-papeis-quarkus`, criado a partir do `develop`; PR de volta
  para `develop`.
- Existe implementação de referência **Spring** no branch
  `feature/openspec-01-autenticacao-e-papeis-implementacao` (não mergear): `domain/`,
  `application/` e migrações Flyway V1/V2 são agnósticos de framework e podem ser copiados
  seletivamente, validando cada arquivo contra o design.md do módulo.
- Restrições fixas do ARCHITECTURE-SPINE (não editar): AD-2 (dependências
  `api → application → domain ← infrastructure`), AD-8 (JWT HS256, claim `role`, matriz),
  AD-9 (`/api/v1/`, login/health públicos), AD-12 (secret `jwtSecret`), AD-14 (JaCoCo ≥ 90%).

## Goals / Non-Goals

**Goals:**

- App Quarkus 3.33 LTS funcional em `feedbacks/apps/api/` com a superfície HTTP e os códigos de
  erro exatos do design do módulo 01.
- Checkboxes do proposal.md §6 do módulo verificáveis; todo cenário de specs.md coberto por
  teste; suíte verde; JaCoCo ≥ 90%; zero dependência Spring.

**Non-Goals:**

- CI/CD (GitHub Actions, Dockerfile), build nativo GraalVM.
- Lógica de negócio dos módulos 02+ (catálogo, inscrição, avaliação) — aqui só stubs de
  autorização (ver decisão D4).
- Refresh token, signup, MFA, recuperação de senha (out of scope do módulo).

## Decisions

### D1 — Bootstrap Maven manual com BOM `quarkus-bom` 3.33 LTS

Projeto Maven único em `feedbacks/apps/api/` com o pom de referência do design.md §12
(rest-jackson, hibernate-orm, jdbc-postgresql, flyway, smallrye-jwt, smallrye-jwt-build,
hibernate-validator, smallrye-health, elytron-security-common; junit5 + rest-assured +
quarkus-jacoco em teste). Fixar a release 3.33.x mais recente disponível. Alternativa
descartada: `quarkus create app` — gera esqueleto genérico que teria de ser reestruturado; o
design já dita pom e pacotes.

### D2 — Cópia seletiva do branch Spring via `git show`

Copiar do branch de referência apenas o que o design confirma como agnóstico:

- `domain/auth/{Usuario,Papel}.java`, `domain/exception/*` — validar contra design §5 e §8.1.
- `application/auth/**` (LoginUseCase, portas `TokenService`/`UsuarioRepository`/
  `CurrentUserProvider`, DTOs `AuthenticatedUser`/`LoginCommand`/`LoginResult`) — validar
  assinaturas contra design §3/§4.2; remover qualquer anotação/import Spring
  (`@Service`, `@Component` etc.) — use cases viram beans CDI (`@ApplicationScoped`) ou são
  produzidos por config, mantendo o pacote `application` livre de framework onde possível.
- `db/migration/V1__create_schema.sql` e `V2__seed_usuarios.sql` — validar contra design §5.3
  (tabela `usuario`, hashes BCrypt no seed, nunca plaintext).

Tudo que é web/segurança/persistência-adapter (AuthController, SecurityConfig,
JwtAuthenticationFilter, GlobalExceptionHandler, JwtTokenService JJWT etc.) é **reescrito** para
JAX-RS/Quarkus conforme design §2/§7/§8. Critério de aceite da cópia: `grep -r "springframework"`
vazio no projeto final.

### D3 — HS256 simétrico: `mp.jwt.verify.publickey.algorithm=HS256` primeiro; fallback documentado

Seguir design §7.1: `smallrye.jwt.verify.secretkey` com JWK `{"kty":"oct","k":base64url(secret)}`
derivado de `app.jwt.secret` em bootstrap (fonte única `jwtSecret` — invariante §9.2),
`quarkus.http.auth.proactive=false`, `smallrye.jwt.path.groups=role`, sem `mp.jwt.verify.issuer`.
Se `mp.jwt.verify.publickey.algorithm=HS256` se provar inviável na prática, usar
`smallrye.jwt.verify.algorithm=HS256` e **registrar a divergência** na seção "Divergências"
desta change (para posterior ajuste do design/Spine) — sem bloquear a entrega.

### D4 — Stubs mínimos para provar a matriz de autorização

SPEC-2.x exige rotas de Curso/Aula/Avaliação/Inscrição que pertencem aos módulos 02+. Para
tornar a matriz testável sem antecipar negócio (mesma abordagem do branch de referência):

- Resources JAX-RS stub com o `@RolesAllowed` exato da matriz §3.3:
  `POST /api/v1/cursos` e `POST /api/v1/cursos/{cursoId}/aulas` (`ADMINISTRADOR`);
  `POST /api/v1/avaliacoes` e `POST /api/v1/cursos/{id}/inscricoes` (`ESTUDANTE`);
  `GET` de catálogo autenticado para ambos os papéis.
- Stubs respondem 201/200 com corpo mínimo (sem persistência de negócio) — os módulos 02+
  substituem o corpo mantendo anotação e path (contrato de autorização estável).
- Exceção: `GET /api/v1/avaliacoes` (SPEC-2.10/2.11) precisa de dado real para provar o filtro
  por papel — read model mínimo de Avaliação (entity + repositório de leitura +
  `ListarAvaliacoesUseCase` usando `CurrentUserProvider`), como no branch de referência. A
  escrita de Avaliação continua stub.

### D5 — Testes em duas camadas + JaCoCo no `verify`

- **Unit (JUnit 5 + Mockito, sem Quarkus):** `LoginUseCaseTest`,
  `ListarAvaliacoesUseCaseTest`, teste do adapter `SmallRyeJwtTokenService`
  (round-trip generate/parse, expirado, assinatura inválida).
- **`@QuarkusTest` + RestAssured:** login 200/401/400 (SPEC-1.1–1.6), 401 de token
  (SPEC-1.7–1.11, incluindo token expirado gerado com `exp` no passado e token assinado com
  outro segredo), 403/permissões da matriz (SPEC-2.1–2.9), filtro de listagem (SPEC-2.10/2.11),
  NFRs (log sem senha via captura de log, hash BCrypt no banco, envelope traceId).
- Banco de teste: Dev Services do Quarkus (Testcontainers PostgreSQL) — mesmas migrações Flyway
  V1/V2 aplicadas; sem H2, para fidelidade com produção.
- JaCoCo (`quarkus-jacoco`) com gate de 90% line coverage no `mvn verify`; exclusões só para
  bootstrap/config pura, documentadas no pom (AD-14).
- Mapeamento cenário→teste rastreável: cada método de teste nomeia o SPEC que cobre
  (ex.: `spec1_3_emailInexistenteRetorna401`).

### D6 — Perfis `%local` / `%aws` sem secret de produção

`application.properties` único: defaults de validação JWT compartilhados; `%local` com
`${JWT_SECRET:dev-only-...}` (default só de dev, permitido pelo design §9.2) e datasource local;
`%aws` com `${JWT_SECRET}`/`${DB_*}` obrigatórios (injetados de Secrets Manager via CDK/ECS —
fora desta change). Testes usam profile `test` com secret fixo de teste.

## Risks / Trade-offs

- [SmallRye rejeitar HS256 via `mp.jwt.verify.publickey.algorithm`] → fallback D3
  (`smallrye.jwt.verify.algorithm=HS256`) com divergência registrada na change.
- [Distinção `AUTH_INVALID_TOKEN` vs `AUTH_TOKEN_EXPIRED` depender da causa raiz interna do
  SmallRye (design §8.3)] → teste dedicado para cada código; se a inspeção da causa se mostrar
  frágil, fazer parse leve do payload (claim `exp`) no mapper apenas para classificar o erro —
  sem revalidar assinatura.
- [Stubs de módulos 02+ virarem contrato acidental] → stubs documentados como provisórios no
  código e nesta change; paths e `@RolesAllowed` são o contrato estável, corpo é substituível.
- [Cobertura ≥ 90% com código de stub] → stubs são triviais e cobertos pelos próprios testes de
  autorização; exclusões JaCoCo restritas a bootstrap documentado.
- [Cópia do branch Spring trazer import/estilo incompatível] → validação arquivo a arquivo
  contra o design + `grep` de guarda contra `springframework` no aceite.

## Migration Plan

Não há migração de dados (projeto greenfield). Flyway V1/V2 criam schema e seed em qualquer
ambiente novo. Rollback = descartar o branch; nenhum ambiente compartilhado é tocado.

## Open Questions

Nenhuma bloqueante. As decisões em aberto do design do módulo (§13) já têm default adotado:
path `/api/v1/auth/login`, sem claim `email`, um Estudante + um Admin no seed, normalização de
email (lowercase/trim) no `LoginUseCase`.
