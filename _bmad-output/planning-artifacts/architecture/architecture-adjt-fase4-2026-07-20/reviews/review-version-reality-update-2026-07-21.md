# Review — Version / Reality Check (update Spring → Quarkus, 2026-07-21)

**Escopo:** re-baseline do Spine pela Sprint Change Proposal 2026-07-21. Verificação restrita ao que a mudança tocou; pins não tocados (PostgreSQL 16.x, OpenPDF 3.0.5, aws-cdk-lib 2.261.0, Kafka 3.x) já haviam sido verificados no run de criação (2026-07-20) e não mudaram.

**Veredito: PASS.**

## Verificado contra a web (2026-07-21)

| Item | Fonte | Resultado |
| --- | --- | --- |
| Quarkus 3.33 é o LTS atual | quarkus.io/releases | Confirmado — lançado 25/03/2026, manutenção comunitária até 25/03/2027, micro atual 3.33.2. Recomendado para produção. |
| Alternativa não-LTS | quarkus.io/releases | 3.37 é o minor ativo mais novo; para prazo curto e estabilidade, LTS 3.33 é a escolha correta. |
| Nomes de extensões | catálogo Quarkus 3.33 | `quarkus-rest-jackson`, `quarkus-hibernate-orm`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-smallrye-jwt`, `quarkus-smallrye-jwt-build`, `quarkus-smallrye-health`, `quarkus-amazon-lambda`, `quarkus-messaging-kafka`, `quarkus-smallrye-fault-tolerance`, `quarkus-jacoco`, `quarkus-hibernate-validator` — todos existem sob esses nomes na linha 3.33. |
| HS256 no SmallRye JWT | docs SmallRye JWT / proposta | Verificação simétrica exige config explícita `mp.jwt.verify.publickey.algorithm=HS256` (default privilegia RSA) — exatamente o que AD-8 fixa. |
| Health path | docs Quarkus SmallRye Health | `/q/health` é o path default — compatível com ALB health check via target group path. |
| Packaging Lambda | docs quarkus-amazon-lambda | Build gera `function.zip` (target/function.zip) — conforme AD-13. |

## Observações (não bloqueantes)

1. A tabela Stack não pinna o micro (3.33.x). Adequado: o BOM `quarkus-bom` na linha LTS recebe micros de segurança; pinnar o micro no spine criaria drift artificial.
2. `smallrye-jwt-build` é transitiva de `quarkus-smallrye-jwt` em versões recentes, mas declarar `quarkus-smallrye-jwt-build` explícito (como o spine faz) é a forma recomendada e à prova de mudança.
