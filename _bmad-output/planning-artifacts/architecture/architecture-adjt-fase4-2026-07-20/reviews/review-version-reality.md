# Version & Reality Review — Architecture Spine

**Document reviewed:** `ARCHITECTURE-SPINE.md`  
**Reviewer role:** Independent version/reality checker  
**Review date:** 2026-07-20  
**Project state:** Greenfield (no application code in repo; planning artifacts only)  
**Mandate:** Verify every committed stack/decision pin against web sources, Initializr live defaults, or existing project — flag unconfirmed or stale assertions.

---

## Verdict

**PASS WITH CAVEATS**

The spine’s **explicit version pins are current and mutually compatible** as of 2026-07-20. Parent-run research on Spring Boot 4.0.7, Spring Cloud 2025.1.2, Spring Cloud Function 5.0.3, JaCoCo 0.8.15, aws-cdk-lib 2.261.0, and Java 17 Lambda runtime was **reconfirmed** and not contradicted.

However, several stack rows are **vague (“aligned to Boot”, “2.x”)** or **omit Boot 4 / Initializr deltas** that implementers will hit on day one. The greenfield starter section does not fully reflect what `start.spring.io` actually generates for Boot 4.0.7. **PDF generation, JWT library choice, Resilience4j module/version, and AWS SDK BOM** are absent or underspecified — not wrong, but not reality-checked in the spine.

---

## Methodology

| Source | Used for |
| --- | --- |
| Prior parent-run web research (2026-07-20) | Boot 4.0.7, Spring Cloud 2025.1.2, SCF 5.0.3, JaCoCo 0.8.15, aws-cdk-lib 2.261.0, Java 17 Lambda |
| Live web search / fetch (this review) | RDS PG 16 sa-east-1, SES sa-east-1, Resilience4j Boot 4, Flyway Boot 4, Kafka Docker tags, OpenPDF, Spring Security JWT Boot 4, Spring Cloud AWS 4.x |
| `start.spring.io` metadata + generated `pom.xml` (curl, 2026-07-20) | Boot/Java defaults, dependency IDs, Boot 4 starter artifact names, cloud-function BOM |
| Repo scan | No `pom.xml` / `build.gradle` application code — greenfield confirmed |
| `.memlog.md` | SES sandbox OK; Spring Boot pin upgraded from draft 3.4.x to 4.0.7 |

---

## Confirmed Version Pins

| Item | Spine value | Status | Evidence |
| --- | --- | --- | --- |
| Spring Boot | 4.0.7 | **Confirmed current** (latest 4.0 patch) | [spring.io blog 2026-06-10](https://spring.io/blog/2026/06/10/spring-boot-4-0-7-available-now); Maven Central / GitHub release v4.0.7 |
| Spring Boot 4.0 OSS support | until Dec 2026 | **Confirmed** | HeroDevs EOL table (July 2026) |
| Spring Cloud BOM | 2025.1.2 (Oakwood) | **Confirmed compatible with Boot 4.0.7** | [Spring blog 2026-06-11](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released); [compatibility wiki](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions) |
| Spring Cloud Function | 5.0.3 | **Confirmed** in 2025.1.2 train | GitHub release v2025.1.2; Maven Central `spring-cloud-function-adapter-aws@5.0.3` |
| JaCoCo | 0.8.15 | **Confirmed latest** (2026-06-04) | [GitHub release v0.8.15](https://github.com/jacoco/jacoco/releases/tag/v0.8.15); supports Java 17+ |
| aws-cdk-lib | 2.261.0 | **Confirmed latest** (2026-07-02) | [npm](https://www.npmjs.com/package/aws-cdk-lib); [GitHub v2.261.0](https://github.com/aws/aws-cdk/releases/tag/v2.261.0) |
| Java (API) | 17 | **Confirmed valid** for Boot 4.0.x | Boot 4 requires Java 17–25; Initializr default Java = 17 |
| Java (Lambda) | java17 | **Confirmed supported** | [AWS Lambda runtimes](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html) — deprecation 2027-06-30 |
| PostgreSQL RDS | 16.x, sa-east-1 | **Confirmed available** | AWS RDS region matrix lists “All PostgreSQL 16 versions” in South America (São Paulo); latest minor ~16.14 (May 2026) |
| SES / SQS / S3 / ECS / EventBridge | sa-east-1 | **Confirmed regional services** | [SES endpoints](https://docs.aws.amazon.com/general/latest/gr/ses.html) includes sa-east-1 |
| ECS Fargate + Spring Boot | pattern valid | **Confirmed current** | Standard deployment pattern; Actuator `/actuator/health` for ALB/task health (industry docs 2026) |

**Note:** Spring Boot **4.1.0** is the **Initializr default** and newer stable line (June 2026). Pinning **4.0.7** is deliberate and still OSS-supported; not stale, but implementers selecting “default” on Initializr will get 4.1.0 unless they change it.

---

## Greenfield Starter Reality Check (`start.spring.io`)

Fetched metadata and generated POMs on **2026-07-20**.

### Initializr defaults vs spine

| Setting | Initializr default (live) | Spine | Gap |
| --- | --- | --- | --- |
| Spring Boot | **4.1.0.RELEASE** | 4.0.7 | Pin is valid but not the UI default |
| Java | **17** | 17 | Match |
| Packaging | jar | (implied) | Match |
| Build tool | **gradle-project** | Unspecified | Spine silent — teams may diverge Maven vs Gradle |

### API project (`dependencies=web,security,data-jpa,validation,actuator,flyway`)

Initializr **does generate** the spine’s dependency intent, but Boot 4 uses **renamed starters**:

| Spine says | Initializr actually adds (Boot 4.0.7) |
| --- | --- |
| Web | `spring-boot-starter-webmvc` (not `spring-boot-starter-web`) |
| Flyway | `spring-boot-starter-flyway` (not bare `flyway-core`) |
| Security, JPA, Validation, Actuator | Same logical starters; Boot 4 also adds `*-test` starters |

**Flyway + PostgreSQL:** Boot 4 docs require `flyway-database-postgresql` for RDS/Postgres in addition to `spring-boot-starter-flyway`. Initializr Flyway checkbox alone does **not** add the Postgres module — implementer must add manually ([Boot 4 data initialization](https://docs.spring.io/spring-boot/4.0/how-to/data-initialization.html), [migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)).

### Lambda project (`dependencies=cloud-function`)

| Expected (AD-13) | Initializr provides | Manual step |
| --- | --- | --- |
| Spring Boot (no Web) | `spring-boot-starter` only | OK |
| Spring Cloud BOM 2025.1.2 | `${spring-cloud.version}=2025.1.2` | OK — matches spine |
| SCF context | `spring-cloud-function-context` | OK |
| AWS adapter | **Not included** | Must add `spring-cloud-function-adapter-aws` + shade plugin per [SCF AWS docs](https://cloud.spring.io/spring-cloud-function/reference/html/aws.html) |
| FunctionInvoker handler | N/A in generated project | Configure in CDK / Lambda console |

**Conclusion:** AD-13 is architecturally sound but **beyond Initializr’s one-click output**. Lambda scaffold is incomplete without adapter-aws and packaging config.

---

## Stack Rows Not Reality-Checked in Spine

### 1. Resilience4j — **UNPINNED / PARTIALLY STALE**

| Spine | Reality (2026-07-20) |
| --- | --- |
| “Resilience4j \| alinhado ao Boot / starter” | Boot 4 requires **`resilience4j-spring-boot4:2.4.0`** — using `resilience4j-spring-boot3` on Boot 4 “works” but **breaks metrics** ([PR #2384](https://github.com/resilience4j/resilience4j/pull/2384)) |
| Not listed in API Initializr deps | Initializr offers **`cloud-resilience4j`** under Spring Cloud (Circuit Breaker), not a direct API dependency — spine does not say which path |

**Risk:** Implementer adds wrong module or skips Resilience4j entirely while spine implies it exists.

**Also:** Boot 4 / Spring Framework 7 ship native `@Retryable` / `@ConcurrencyLimit`. Resilience4j is optional for circuit breaker / bulkhead — spine does not clarify whether Resilience4j is required for MVP or optional.

### 2. Spring Security + JWT — **UNSPECIFIED LIBRARY / PATTERN**

| Spine | Reality |
| --- | --- |
| “Spring Security + JWT \| alinhado ao Boot 4.0.7” | Login **issuing** JWT (FR-1) is not the same as OAuth2 Resource Server validating external tokens |
| No jjwt / Nimbus / Authorization Server choice | Boot 4 renamed starters: `spring-boot-starter-security-oauth2-resource-server`, etc. ([migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)) |
| Custom `role` claim `ESTUDANTE` \| `ADMINISTRADOR` | Boot 4.1 adds `authorities-claim-expressions` for complex JWT role extraction — spine predates naming but pattern is valid on 4.0.7 with custom `JwtDecoder` / manual token service |

**Risk:** Two implementers could choose incompatible JWT stacks (manual HMAC service vs Spring Authorization Server vs Resource Server self-validation).

### 3. AWS SDK for Java — **VAGUE “2.x”**

| Spine | Reality |
| --- | --- |
| “AWS SDK for Java \| 2.x” | Current SDK BOM on GitHub README: **2.47.0** (July 2026) |
| No Spring Cloud AWS mention | **Spring Cloud AWS 4.0.x** targets Boot 4 / Spring Cloud 5; bundles SDK ~2.39+ ([release 4.0.0](https://github.com/awspring/spring-cloud-aws/releases/tag/v4.0.0)) |

**Risk:** Raw SDK versions drift; duplicate AWS client config between API and Lambdas. Spine neither pins BOM nor states Spring Cloud AWS vs manual SDK clients.

### 4. PDF generation — **ABSENT FROM STACK**

| Spine | Reality |
| --- | --- |
| AD-6 / conventions mention PDF → S3 | **No PDF library** in Stack table |
| “tabelas com agregados” (OQ-5) | Common Java 17 choices: **OpenPDF 3.0.5** (LGPL/MPL, Java 17 branch 2.0.x+) or OpenHTMLToPDF for HTML templates ([OpenPDF GitHub](https://github.com/LibrePDF/OpenPDF)) |

**Risk:** lambda-report epic starts without an agreed dependency; iText 7 AGPL trap possible if chosen ad hoc.

### 5. Apache Kafka — **UNDERSPECIFIED “3.x”**

| Spine | Reality |
| --- | --- |
| “Apache Kafka \| 3.x (Compose local only)” | Docker Hub `apache/kafka`: **3.9.2**, **4.3.1** (latest, KRaft-only) |
| `docker-compose.yml` mentioned, no image tag | Kafka **4.x removes ZooKeeper**; compose must use KRaft env vars ([apache/kafka Docker](https://hub.docker.com/r/apache/kafka)) |
| Spring adapter | Initializr `kafka` → `spring-boot-starter-kafka` (works with local broker) |

**Risk:** Copy-paste ZooKeeper-based compose from old tutorials fails on Kafka 4.x images.

### 6. Jackson / JSON — **SILENT Boot 4 DEFAULT**

Boot 4 defaults to **Jackson 3** ([Boot 4 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)). Spine camelCase JSON convention is fine but implementers migrating from Boot 3 snippets may hit import/package changes. **Not wrong — not documented.**

---

## Operational Reality (Not Version Pins, But Deployment Truth)

| Topic | Spine / memlog | Reality check |
| --- | --- | --- |
| SES in sa-east-1 | Used for alert + report email | Region available; **new accounts start in sandbox** (200 emails/day, verified recipients only) — memlog accepts sandbox for academic demo |
| SES production access | Not in spine | Manual request per region; student/new accounts may face delays ([AWS docs](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html)) |
| Java 17 Lambda on AL2 | java17 runtime | Supported until **2027-06-30**; AWS plans **AL2023-based java17** by end of July 2026 ([Lambda runtimes](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html)) — no action for MVP, note for teardown/redeploy |
| Spring Boot 4.0.7 vs 4.1.0 | Pin 4.0.7 | 4.1.0 is newer; staying on 4.0.7 is support-valid until Dec 2026 |
| CDK 2.261.0 breaking changes | Pin only | Release includes L1 CFN definition breaks (CloudWatch, ELB, RDS Secrets) — synth tests advised ([changelog](https://github.com/aws/aws-cdk/compare/v2.260.0...v2.261.0)) |

---

## AD / Decision vs Training-Data Drift

| AD / area | Training-data risk | Review finding |
| --- | --- | --- |
| AD-13 SCF + Lambda | Old samples use `SpringBootStreamHandler`, Java 11 | **Current:** `FunctionInvoker::handleRequest`, SCF **5.0.3**, shade JAR — spine is correct |
| AD-12 CDK TypeScript | ASSUMPTION tag present | Valid; CDK Java equally supported — not a version issue |
| AD-14 JaCoCo 90% | Old JaCoCo + Java bytecode issues | **0.8.15** supports Java 17; gate is feasible |
| Starter “Web, Security, …” | Boot 3 artifact names | **Boot 4 rename drift** — see Initializr section |
| Flyway | Boot 3: flyway-core on classpath | **Boot 4: explicit `spring-boot-starter-flyway`** required |

---

## Findings Summary

| ID | Severity | Finding | Recommendation |
| --- | --- | --- | --- |
| VR-1 | **Medium** | Resilience4j unpinned; wrong module breaks metrics on Boot 4 | Add `resilience4j-spring-boot4:2.4.0` to stack **or** drop Resilience4j and document Boot 4 native `@Retryable` for SQS publish retry |
| VR-2 | **Medium** | JWT issuance library/pattern not specified | Add AD or stack row: e.g. custom `JwtService` (Nimbus/JJWT) + Security filter chain **or** `spring-boot-starter-security-oauth2-authorization-server` for login |
| VR-3 | **Medium** | PDF library missing from stack | Pin **OpenPDF 3.0.5** (or OpenHTMLToPDF) for `lambda-report` |
| VR-4 | **Medium** | Boot 4 starter rename + Flyway Postgres module not reflected in Starter section | Update Structural Seed / onboarding: `webmvc`, `starter-flyway`, `flyway-database-postgresql`, `*-test` starters |
| VR-5 | **Low** | Kafka “3.x” + compose unspecified | Pin image e.g. `apache/kafka:3.9.2` with KRaft compose snippet |
| VR-6 | **Low** | AWS SDK “2.x” vague | Pin `spring-cloud-aws-dependencies:4.0.2` **or** `software.amazon.awssdk:bom:2.47.0` |
| VR-7 | **Low** | Initializr default Boot 4.1.0 vs pin 4.0.7 | Document “select 4.0.7 explicitly on start.spring.io” |
| VR-8 | **Info** | Initializr `cloud-function` omits adapter-aws | Document as mandatory post-Initializr step (aligns with AD-13) |
| VR-9 | **Info** | Java 17 Lambda AL2 → AL2023 migration mid-2026 | No MVP blocker; mention in ops/teardown doc |

---

## Items Explicitly NOT Flagged (Confirmed OK)

- Spring Boot 4.0.7 existence and patch currency  
- Spring Cloud 2025.1.2 ↔ Boot 4.0.7 compatibility  
- Spring Cloud Function 5.0.3 + adapter-aws on Maven Central  
- JaCoCo 0.8.15 as latest  
- aws-cdk-lib 2.261.0 as latest npm release  
- Java 17 Lambda runtime still active  
- PostgreSQL 16 on RDS sa-east-1  
- SES available in sa-east-1  
- ECS Fargate as Spring Boot host (valid pattern)  
- GitHub Actions → ECR → CDK deploy (mainstream; no version conflict)

---

## Recommended Spine Patches (Minimal)

1. **Stack table** — add rows: Resilience4j module/version (or “deferred — use Boot 4 @Retryable”), PDF library, AWS integration BOM, Kafka Docker image tag.  
2. **Starter section** — replace “Web” with “WebMVC (`spring-boot-starter-webmvc`)"; note Flyway starter + `flyway-database-postgresql`.  
3. **Lambda bootstrap note** — Initializr `cloud-function` + manual `spring-cloud-function-adapter-aws` + shade plugin.  
4. **JWT** — one-line pattern decision (custom login JWT vs authorization server).

---

## Reviewer Sign-off

| Criterion | Result |
| --- | --- |
| Explicit pins web-verified | **Yes** (all named versions) |
| Technologies still exist & fit | **Yes** |
| Greenfield Initializr defaults checked | **Yes** (metadata + live POM) |
| Unconfirmed assertions flagged | **Yes** (see VR-1–VR-9) |
| Contradiction with parent research | **None** |

**Final verdict: PASS WITH CAVEATS** — core pins are solid; implementer-facing gaps are in underspecified dependencies and Boot 4 starter renames, not in wrong version numbers.
