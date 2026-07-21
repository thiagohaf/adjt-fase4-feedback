# Review — Adversarial (update Spring → Quarkus, 2026-07-21)

**Ataque:** construir duas unidades um nível abaixo que obedecem cada AD à letra e ainda assim divergem, focando nos pontos que a troca de stack tocou.

**Veredito: PASS** — nenhuma incompatibilidade nova aberta pela mudança; os pares atacados abaixo continuam fechados pelos ADs existentes.

## Pares atacados

1. **API emite JWT (smallrye-jwt-build) × API valida JWT (quarkus-smallrye-jwt).** Emissor poderia assinar RS256 (default SmallRye) enquanto o validador espera HS256. Fechado: AD-8 fixa HS256 nos dois lados com `mp.jwt.verify.publickey.algorithm=HS256` explícito e o secret único `jwtSecret` (AD-12) — um emissor RS256 viola a Rule, não só a intenção.
2. **Adapter Kafka (%local) × adapter SQS (%aws).** Times independentes poderiam serializar payloads distintos por broker. Fechado: AD-5 + AD-7 (mesmo JSON, porta `EvaluationEventPublisher`) não mudaram; `quarkus-messaging-kafka` vs SQS SDK v2 é detalhe de adapter atrás da mesma porta.
3. **`lambda-notification` × `lambda-report` (empacotamento/handler).** Uma poderia usar handler REST (quarkus-amazon-lambda-rest) e outra o handler puro, divergindo no formato do evento. Fechado: AD-13 reescrito exige `quarkus-amazon-lambda` (handler de evento) e proíbe servidor HTTP embutido nas duas.
4. **Health da API × target group do ALB.** O default Quarkus `/q/health` poderia divergir do path configurado no CDK. Fechado: AD-11 agora nomeia `/q/health` e exige exposição em path compatível com o ALB health check — o CDK (AD-12/AD-17) configura o target group a partir desse AD.
5. **Retry SmallRye FT na API × retry nativo SQS/Lambda.** Retries duplicados poderiam gerar alerta duplicado. Já coberto pelo desenho anterior (não é regressão da troca): AD-4 publica após commit com DLQ; e-mail duplicado em retry é tolerado no MVP — mesma semântica do Resilience4j antes.
6. **`domain`/`application` copiados do branch Spring × casca Quarkus nova.** Cópia poderia arrastar imports Spring (`@Service`, `@Transactional` do Spring). Fechado: AD-2 proíbe `domain` depender de framework; a Prevents atualizada (JAX-RS/Hibernate) mantém o mesmo teto. Vale vigilância na revisão de código do Passo 4 (fora do escopo deste update).

## Observação (não bloqueante)

- AD-2 Prevents cita "JAX-RS/Hibernate" como exemplos; a regra geral (dependência só para dentro) é o que fecha o caso. Nenhuma ação necessária.
