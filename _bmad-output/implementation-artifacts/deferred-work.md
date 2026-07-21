# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/spec-rebaseline-quarkus-spec-modulo-01.md`
  summary: Validar na implementação (Passo 4) se a redação literal do AD-8 do Spine ("via `mp.jwt.verify.publickey.algorithm=HS256`") funciona ou se o Spine deve ser ajustado para `smallrye.jwt.verify.algorithm=HS256`.
  evidence: O guia oficial Quarkus JWT RBAC documenta `mp.jwt.verify.publickey.algorithm` para algoritmos assimétricos (RS256/ES256) e indica `smallrye.jwt.verify.algorithm` para simétricos (HS256); o par exato propriedade/valor do AD-8 é duvidoso segundo os docs, mas é invariante do Spine re-baselineado — corrigir exige tocar o Spine, fora do escopo do Passo 3.

- source_spec: `_bmad-output/implementation-artifacts/spec-rebaseline-quarkus-spec-modulo-01.md`
  summary: Definir o mecanismo concreto de exposição do health check no path de contrato `/api/v1/health` (ex.: `quarkus.smallrye-health.root-path` vs resource fino delegando ao check) — decisão pertence ao módulo de observabilidade.
  evidence: SPEC-1.11 exige `GET /api/v1/health` público; SmallRye Health expõe `/q/health` por default; o design do módulo 01 apenas referencia o contrato (§7.2/§14) e adia o "como" — decisão sem dono até o módulo de observabilidade ser especificado.
