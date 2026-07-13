# Execution Plan: WP-S0-03 - PostgreSQL and Liquibase Baseline

- Status: In review
- Owner: Codex / Mohamed
- Started: 2026-07-06
- Last updated: 2026-07-06
- Backlog item: WP-S0-03 PostgreSQL and Liquibase baseline
- Requirement IDs: TEST-04
- Source sections: 14-15, 30

## Goal and observable outcome

The backend starts against a real PostgreSQL Testcontainer, applies the root
Liquibase changelog, and keeps Hibernate in `ddl-auto=validate` mode so schema
changes are migration-owned.

## In scope

- Add backend dependencies for JDBC/JPA, Liquibase, PostgreSQL, and
  Testcontainers.
- Add application persistence configuration with environment-driven datasource
  defaults, Liquibase root changelog, and `ddl-auto=validate`.
- Add a root Liquibase changelog with initial audit conventions and no business
  tables yet.
- Add `LiquibaseMigrationIT` proving the changelog applies to PostgreSQL.
- Keep the smoke context test aligned with PostgreSQL Testcontainers.

## Out of scope

- Domain table migrations for identity/master-data/imports.
- Repositories, JPA entities, Docker Compose, seed data, or runtime production
  database provisioning.
- H2 or any non-PostgreSQL substitute.

## Current-state findings

- Backend currently has a minimal Spring Boot application and one context test.
- No datasource, Liquibase, PostgreSQL driver, Testcontainers, or resources
  exist yet.
- `mvn clean verify` passes before this task.

## Normative decisions and source conflicts

- The test strategy explicitly says not to use H2 as a substitute for
  PostgreSQL integration tests.
- Domain model requires UUID primary keys, explicit PostgreSQL foreign keys,
  Liquibase-owned schema, and audit conventions.
- No source conflict was found.

## Milestones

### Milestone 1 - Persistence dependencies and config

- [x] Add Spring Data JPA, Liquibase, PostgreSQL driver, and Testcontainers.
- [x] Add application YAML with Liquibase changelog and Hibernate validate.

### Milestone 2 - Liquibase baseline

- [x] Add root changelog.
- [x] Add initial migration documenting PostgreSQL extensions and audit
  conventions without adding business behavior.

### Milestone 3 - PostgreSQL verification

- [x] Add `LiquibaseMigrationIT`.
- [x] Run `mvn "-Dtest=LiquibaseMigrationIT" verify`.
- [x] Run backend `mvn clean verify`.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Liquibase applies to PostgreSQL | Integration | `LiquibaseMigrationIT` | Pass |
| Hibernate schema management is validate-only | Integration | `LiquibaseMigrationIT` | Pass |
| Application context starts with PostgreSQL | Integration | `WerkPilotApplicationTests` | Pass |

## Risks and rollback

- Data/migration risk: low; only baseline extension/convention migration.
- Security risk: low; no secrets are committed, datasource uses environment
  defaults.
- Compatibility risk: Testcontainers requires a working local Docker runtime.
- Rollback or safe recovery: revert the dependency/config/changelog/test files.

## Progress log

- 2026-07-06 - Started WP-S0-03 and inspected backend skeleton.
- 2026-07-06 - Added Spring Data JPA, Spring Boot Liquibase, PostgreSQL driver,
  Testcontainers 2.0.2 BOM/modules, and Surefire includes for `*IT`.
- 2026-07-06 - Added `application.yml` with environment-driven datasource
  defaults, Liquibase root changelog, and Hibernate `ddl-auto=validate`.
- 2026-07-06 - Added baseline changelog enabling `pgcrypto` and tagging the
  database `wp-s0-03-baseline`.
- 2026-07-06 - Added shared PostgreSQL Testcontainer support and
  `LiquibaseMigrationIT`.
- 2026-07-06 - Initial Testcontainers 1.21.3 attempt could not connect cleanly
  to Docker Desktop 4.80 / Docker 29.6.1; upgraded to Testcontainers 2.0.2 and
  renamed modules (`testcontainers-junit-jupiter`,
  `testcontainers-postgresql`).
- 2026-07-06 - Initial Liquibase attempt with `liquibase-core` alone did not
  trigger Boot 4 autoconfiguration; replaced it with
  `spring-boot-starter-liquibase`.
- 2026-07-06 - `mvn "-Dtest=LiquibaseMigrationIT" verify` passed. It applied
  2 Liquibase changesets to PostgreSQL 16 Testcontainer and verified
  `pgcrypto`.
- 2026-07-06 - `mvn clean verify` passed. Surefire ran
  `LiquibaseMigrationIT` and `WerkPilotApplicationTests` with 2 tests, 0
  failures, 0 errors.
- 2026-07-06 - `powershell -File scripts\validate-agent-kit.ps1` passed.

## Decision log

- 2026-07-06 - Use PostgreSQL Testcontainers for context and Liquibase tests to
  honor the no-H2 test strategy.
- 2026-07-06 - Keep the baseline migration behavior-only: PostgreSQL extension
  and Liquibase tag, no business tables before reviewed domain migrations.

## Unexpected findings

- Backend verification still emits the non-blocking Mockito/ByteBuddy
  dynamic-agent warning on Java 25.

## Final verification

- [x] Focused tests: `mvn "-Dtest=LiquibaseMigrationIT" verify` passed.
- [x] Backend `clean verify` when affected: `mvn clean verify` passed.
- [x] Frontend lint/test/build when affected: not affected by WP-S0-03.
- [x] Docker/health/E2E checks when affected: Docker/Testcontainers path
  covered by backend integration tests; Compose/E2E not affected.
- [x] Diff reviewed for unrelated changes:
- [x] Documentation and traceability updated:

## Handoff

WP-S0-03 established the PostgreSQL/Liquibase baseline and verified it against
PostgreSQL Testcontainers. The next smallest Sprint 0 task is WP-S0-04 Modular
package skeleton, subject to Mohamed's ordering approval.
