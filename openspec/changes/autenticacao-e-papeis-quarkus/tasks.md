# Tasks — Implementar Módulo 01: Autenticação e Papéis (Quarkus)

> Referências: specs desta change (`specs/auth-*/spec.md`), design desta change (`design.md`,
> decisões D1–D6) e o design do módulo `openspec/modules/01-autenticacao-e-papeis/design.md`.

## 1. Bootstrap do projeto (D1)

- [ ] 1.1 Confirmar branch `feature/openspec-01-autenticacao-e-papeis-quarkus` a partir do `develop` atualizado; remover resíduo untracked `feedbacks/apps/api/target/` se existir
- [ ] 1.2 Criar `feedbacks/apps/api/pom.xml` com BOM `quarkus-bom` 3.33 LTS (release mais recente), Java 17 e dependências do design.md §12 do módulo (sem Spring); adicionar `.gitignore` para `target/`
- [ ] 1.3 Criar esqueleto de pacotes `com.fiap.feedbacks.{api.web,application,domain,infrastructure}` conforme design §2 e verificar que `mvn -q compile` passa

## 2. Domínio e aplicação — cópia seletiva do branch Spring (D2)

- [ ] 2.1 Copiar via `git show feature/openspec-01-autenticacao-e-papeis-implementacao:<path>` os arquivos `domain/auth/{Usuario,Papel}.java` e `domain/exception/*`, validando contra design §5/§8.1 (sem imports de framework)
- [ ] 2.2 Copiar `application/auth/**` (LoginUseCase, portas TokenService/UsuarioRepository/CurrentUserProvider, DTOs AuthenticatedUser/LoginCommand/LoginResult), removendo qualquer anotação/import Spring e validando assinaturas contra design §3/§4.2; normalização de email (lowercase/trim) no LoginUseCase
- [ ] 2.3 Copiar migrações `V1__create_schema.sql` e `V2__seed_usuarios.sql` para `src/main/resources/db/migration/`, validando contra design §5.3 (tabela `usuario`, seed `estudante@demo.fiap`/`admin@demo.fiap` com hash BCrypt, nunca plaintext)

## 3. Infraestrutura — persistência e segurança (adapters Quarkus)

- [ ] 3.1 Implementar `UsuarioEntity`, `UsuarioJpaRepository` e adapter `JpaUsuarioRepository` (Hibernate ORM) conforme design §2
- [ ] 3.2 Implementar adapter `PasswordEncoder` BCrypt com `BcryptUtil` (`quarkus-elytron-security-common`)
- [ ] 3.3 Implementar `JwtProperties` (`@ConfigMapping(prefix = "app.jwt")`: `secret()`, `expiration()`) conforme design §9.1
- [ ] 3.4 Implementar `SmallRyeJwtTokenService` (adapter `TokenService` via `smallrye-jwt-build`): `generate` com claims `sub`/`role`/`iat`/`exp` HS256; `parse` lançando `InvalidTokenException`/`TokenExpiredException`
- [ ] 3.5 Implementar bootstrap da chave de validação: derivar JWK simétrico `{"kty":"oct","k":base64url(app.jwt.secret)}` e alimentar `smallrye.jwt.verify.secretkey` (fonte única `jwtSecret` — invariante design §9.2)
- [ ] 3.6 Implementar `SecurityIdentityCurrentUserProvider` (adapter `CurrentUserProvider` sobre `SecurityIdentity`/`JsonWebToken`)

## 4. Camada web — resource, erros e configuração (D3)

- [ ] 4.1 Implementar `AuthResource` (`POST /api/v1/auth/login`, `@PermitAll`) com DTOs `LoginRequest` (Bean Validation: `@NotBlank @Email` email; `@NotBlank @Size(min=6)` password) e `LoginResponse` (`accessToken`, `tokenType`, `expiresIn`)
- [ ] 4.2 Implementar `TraceIdFilter` (`ContainerRequestFilter`): gera ou propaga `X-Trace-Id`, armazena em MDC
- [ ] 4.3 Implementar `ApiErrorResponse` e `ExceptionMapper`s: `InvalidCredentialsException`→401 `AUTH_INVALID_CREDENTIALS`; `ConstraintViolationException`→400 `VALIDATION_ERROR`; `UnauthorizedException`→401 `AUTH_MISSING_TOKEN`; `AuthenticationFailedException`→401 `AUTH_INVALID_TOKEN`/`AUTH_TOKEN_EXPIRED` (distinção pela causa raiz, design §8.3); `ForbiddenException`/`ForbiddenAccessException`→403 `AUTH_FORBIDDEN`
- [ ] 4.4 Configurar `application.properties`: `quarkus.http.auth.proactive=false`, `mp.jwt.verify.publickey.algorithm=HS256`, `smallrye.jwt.path.groups=role`, sem `mp.jwt.verify.issuer`; `app.jwt.expiration=24h`; profiles `%local` (default dev de secret + datasource local) e `%aws` (`${JWT_SECRET}` sem default); se HS256 exigir `smallrye.jwt.verify.algorithm=HS256`, aplicar e registrar divergência na change
- [ ] 4.5 Expor health público em `/api/v1/health` (SmallRye Health) e garantir login/health fora de qualquer `@RolesAllowed` (AD-9)

## 5. Matriz de autorização — stubs e listagem de Avaliações (D4)

- [ ] 5.1 Criar resources stub com `@RolesAllowed` da matriz §3.3: `POST /api/v1/cursos` e `POST /api/v1/cursos/{cursoId}/aulas` (`ADMINISTRADOR`); `POST /api/v1/avaliacoes` e `POST /api/v1/cursos/{id}/inscricoes` (`ESTUDANTE`); `GET` de catálogo autenticado para ambos os papéis — corpos mínimos, documentados como provisórios
- [ ] 5.2 Implementar read model mínimo de Avaliação (`AvaliacaoEntity`, repositório de leitura, `ListarAvaliacoesUseCase` com filtro por papel via `CurrentUserProvider`) e `GET /api/v1/avaliacoes` com `@RolesAllowed({"ESTUDANTE","ADMINISTRADOR"})` (SPEC-2.10/2.11)

## 6. Testes — cada cenário dos specs vira teste (D5)

- [ ] 6.1 Unit (JUnit 5 + Mockito): `LoginUseCaseTest` (credenciais OK, email inexistente, senha errada, normalização de email), `ListarAvaliacoesUseCaseTest` (admin vê todas / estudante vê próprias), `SmallRyeJwtTokenServiceTest` (round-trip, expirado, assinatura inválida)
- [ ] 6.2 `@QuarkusTest` + RestAssured — auth-login: SPEC-1.1, 1.2 (200 + claims decodificadas), SPEC-1.3, 1.4 (401 `AUTH_INVALID_CREDENTIALS`), SPEC-1.5, 1.6 (400 `VALIDATION_ERROR`); Dev Services PostgreSQL com Flyway V1/V2
- [ ] 6.3 `@QuarkusTest` + RestAssured — auth-token-validation: SPEC-1.7 (token válido), SPEC-1.8 (`AUTH_MISSING_TOKEN`), SPEC-1.9 + assinatura divergente (`AUTH_INVALID_TOKEN`), SPEC-1.10 (`AUTH_TOKEN_EXPIRED`), SPEC-1.11 (login/health públicos), traceId no envelope + propagação de `X-Trace-Id`
- [ ] 6.4 `@QuarkusTest` + RestAssured — auth-role-authorization: SPEC-2.1–2.9 (matriz 403/permitido) e SPEC-2.10/2.11 (filtro de listagem com dados de dois estudantes)
- [ ] 6.5 NFRs: SPEC-NFR-1 (config `%aws` sem default de secret), SPEC-NFR-2 (captura de log em login sem `password`/JWT completo), SPEC-NFR-3 (hash BCrypt no banco consultado via repositório)

## 7. Qualidade e critério de saída

- [ ] 7.1 Configurar JaCoCo (`quarkus-jacoco`) com gate ≥ 90% line coverage no `mvn verify`; exclusões apenas para bootstrap/config pura, documentadas no pom (AD-14)
- [ ] 7.2 Rodar `mvn verify` — suíte 100% verde com cobertura ≥ 90%; verificar `grep -r "springframework" feedbacks/apps/api` vazio
- [ ] 7.3 Conferir os checkboxes do proposal.md §6 do módulo 01 um a um contra os testes/execução local e registrar o mapeamento na change
- [ ] 7.4 Registrar na change eventuais divergências encontradas (ex.: config HS256 alternativa de 4.4) e commitar em commits convencionais; abrir PR para `develop`
