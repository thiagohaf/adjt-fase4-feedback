# Divergências — autenticacao-e-papeis-quarkus

Registro de desvios em relação ao design do módulo / Spine, encontrados na implementação
(tarefa 4.4 / D3 / 7.4).

## D-HS256 — algoritmo de verificação

**Design:** `mp.jwt.verify.publickey.algorithm=HS256` (módulo design §7.1).

**Implementação:** `smallrye.jwt.verify.algorithm=HS256`.

**Motivo:** com `mp.jwt.verify.publickey.algorithm=HS256`, o SmallRye JWT 4.x trata a
configuração como “public key presente” e **ignora** `smallrye.jwt.verify.secretkey`
(warning `SRJWT03007`), fazendo a validação falhar para tokens HS256 emitidos pela API.

**Impacto:** nenhum no contrato externo (claims, HS256, claim `role`). Apenas a chave de
configuração MicroProfile/SmallRye muda.

## D-DEFAULTS — defaults Quarkus de demo

**Design:** sem `mp.jwt.verify.issuer`; secret via `app.jwt.secret` / JWK derivado.

**Implementação:** forçar explicitamente:

```properties
mp.jwt.verify.publickey=NONE
mp.jwt.verify.issuer=NONE
```

**Motivo:** a extensão `quarkus-smallrye-jwt` injeta defaults de desenvolvimento
(`mp.jwt.verify.publickey` = RSA de demo, `mp.jwt.verify.issuer=https://quarkus.io/issuer`),
que conflitam com HS256 simétrico e com o contrato sem `iss`.

## D-TRACE — TraceIdFilter em duas camadas

**Design:** `ContainerRequestFilter` (TraceIdFilter) com MDC.

**Implementação:** filtro Vert.x (`TraceIdFilterRegistrar` via `Filters`) + filtro JAX-RS
`@PreMatching` complementar. `ApiErrorFactory` lê MDC e, em fallback, o contexto local Vert.x.

**Motivo:** com `quarkus.http.auth.proactive=false`, falhas 401/403 do Quarkus Security
podem ocorrer fora do ciclo completo dos response filters JAX-RS; o filtro HTTP garante
`X-Trace-Id` e `traceId` no envelope nesses casos.
