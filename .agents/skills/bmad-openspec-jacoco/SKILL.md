---
name: bmad-openspec-jacoco
description: >-
  Audita JaCoCo (Line/Branch), cruza lacunas com OpenSpec e completa testes
  JUnit 5 + Mockito até 90%+. Use quando o build falhar no jacoco:check,
  cobertura estiver abaixo de 90%, o usuário pedir auditoria de cobertura,
  ou em pipelines BMAD (dev-story/QA) e OpenSpec (apply-change) após
  implementação Java/Maven.
---

# Skill: Auditoria e Adequação de Cobertura JaCoCo (BMAD + OpenSpec)

## Objetivo

Mapear lacunas de cobertura (`missed` branches/lines) no relatório JaCoCo e complementar os testes unitários (`JUnit 5 + Mockito`) alinhados às especificações OpenSpec do projeto, visando atingir o limiar mínimo de **90% em Branch e Line Coverage**.

**Your Role:** Você é um engenheiro de qualidade focado em cobertura unitária de domínio/aplicação. Não faça code review de story (use `bmad-code-review`) nem E2E (use `bmad-qa-generate-e2e-tests`).

## Conventions

- Bare paths resolvem a partir do skill root.
- `{skill-root}` = diretório desta skill; `{project-root}` = raiz do repositório.
- Módulos Maven alvo: `{project-root}/feedbacks/apps/api` e `{project-root}/feedbacks/apps/notification`.
- Relatório canônico: `{módulo}/target/jacoco-report/jacoco.xml` (fallback: `{módulo}/target/site/jacoco/jacoco.xml`).
- Gate AD-14 no POM: `jacoco:check` na fase `verify` (LINE ≥ 0.90). Esta skill ainda exige **Branch ≥ 90%** no XML, mesmo que o POM só falhe em LINE.

## On Activation

1. Se existir `{skill-root}/customize.toml`, rode:
   `python3 {project-root}/_bmad/scripts/resolve_customization.py --skill {skill-root} --key workflow`
   e carregue `{workflow.persistent_facts}` (entradas `file:` = globs sob `{project-root}`).
2. Identifique o módulo Maven afetado (`api` e/ou `notification`) pelo contexto da conversa, arquivos abertos ou falha do build.
3. Não cumprimente longamente — entre no fluxo.

## Fluxo de Ação

### 1. Leitura de Métrica & Especificação

1. Localize o XML:
   - Preferir `**/target/jacoco-report/jacoco.xml`
   - Fallback `**/target/site/jacoco/jacoco.xml`
2. Se o XML não existir, rode no módulo:
   ```bash
   mvn clean verify
   ```
   (gera relatório e aplica o gate). Continue mesmo se o build falhar por cobertura.
3. Se disponível, leia as especificações do caso de uso em:
   - `openspec/changes/<change-ativo>/` (deltas, specs, design, tasks)
   - `openspec/specs/`
   - `openspec/modules/`
   Foque em cenários felizes, exceções e NFRs de cobertura (AD-14).

### 2. Filtro de Componentes (Exclusão do Boilerplate)

**Priorize testes para:** classes em `application` (use cases / serviços), `domain` e lógica de negócio Java.

**Não adicione testes para:**

- DTOs, Entities, Records, Enums simples
- Mappers gerados pelo MapStruct
- Classes com `@Configuration` / bootstrap de config

Se essas classes aparecerem com falhas de cobertura no XML, **sugira** marcá-las com `@Generated` ou adicioná-las no `<excludes>` do `jacoco-maven-plugin` no `pom.xml` — não escreva testes só para “passar na métrica”.

### 3. Identificação de Misbehaviors & Uncovered Branches

1. No `jacoco.xml`, mapeie classes/métodos com `counter` LINE ou BRANCH onde `missed > 0`.
2. Cruze com cenários declarados no OpenSpec:
   - **Misbehavior / cenário ausente:** requisito ou exceção da spec sem teste correspondente → prioridade alta.
   - **Edge / condicional:** ramo de `if`/`switch`/`Optional`/`try-catch`/lambda sem cenário de negócio explícito → prioridade média (ainda obrigatório para Branch ≥ 90%).
3. Liste um plano curto (classe → cenários a adicionar) antes de editar.

### 4. Implementação dos Testes Faltantes

- Crie ou atualize testes em `{módulo}/src/test/java/`.
- Stack: **JUnit 5 + Mockito** (`@ExtendWith(MockitoExtension.class)`) + AssertJ, alinhado aos testes existentes.
- Estrutura **Given-When-Then** (ou Arrange-Act-Assert) em cada método.
- Cubra:
  - Fluxos felizes
  - Exceções (`assertThrows`)
  - Valores nulos/vazios e bordas de condicionais
- Nomes de método orientados a cenário (ex.: `sucessoPersisteERetornaAvaliacao`, `rejeitaQuandoAulaNaoInscrita`).
- Não altere código de produção só para inflar cobertura; se o ramo for dead/unreachable, documente e prefira exclusão/`@Generated` quando for boilerplate.

### 5. Loop de Verificação (obrigatório)

Repita até os critérios de saída:

```bash
# no módulo Maven afetado (api ou notification)
mvn clean verify
```

**Critérios de saída:**

- [ ] Build verde (`jacoco:check` OK)
- [ ] Line Coverage ≥ 90% no XML (BUNDLE)
- [ ] Branch Coverage ≥ 90% no XML (BUNDLE)

Se ainda houver `missed > 0` relevante em application/domain após o verify:

1. Releia o `jacoco.xml` atualizado
2. Adicione/ajuste cenários
3. Reexecute `mvn clean verify`

**Não** declare a skill concluída com build vermelho por JaCoCo ou Branch < 90%.

### 6. Resumo ao usuário

Ao finalizar, reporte:

- Módulo(s) auditados
- Line/Branch finais (%)
- Classes cobertas / exclusões sugeridas
- Arquivos de teste criados ou alterados
- Comando de revalidação: `mvn clean verify`

## Integração com pipelines BMAD e OpenSpec

Qualquer agente nestes fluxos **deve** invocar esta skill automaticamente quando:

| Pipeline | Momento |
|----------|---------|
| `bmad-dev-story` | Após implementação: `mvn verify` falha por JaCoCo **ou** Line/Branch < 90% |
| `bmad-qa-generate-e2e-tests` | Se cobertura unitária do módulo estiver abaixo do limiar |
| `openspec-apply-change` | Após tasks Java/Maven: mesma condição |

Loop padrão (independente do pipeline):

1. Rodar `mvn clean verify` no módulo.
2. Se falhar o gate JaCoCo ou Branch < 90% → executar **esta skill** (ler XML + OpenSpec → testes → re-verify).
3. Só então retomar o pipeline (marcar story/tasks, CR, archive, etc.).

## Guardrails

- Foque em Service / Use Case / Domain — não em DTO/Entity/Mapper/Config.
- Prefira poucos testes significativos a dezenas de asserts triviais.
- Preserve o estilo dos testes já existentes no módulo.
- Não faça commit nem push a menos que o usuário peça.
- Se ambos `api` e `notification` estiverem abaixo do limiar, trate um módulo por vez e reporte ambos.
