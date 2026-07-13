# Execution Plan: WP-S0-04 - Modular Package Skeleton

- Status: In review
- Owner: Codex / Mohamed
- Started: 2026-07-06
- Last updated: 2026-07-06
- Backlog item: WP-S0-04 Modular package skeleton
- Requirement IDs: architecture baseline sections
- Source sections: 10-12

## Goal and observable outcome

Create the required `com.werkpilot` backend module/layer package skeleton and
make the package dependency rules executable through a focused architecture
test.

## In scope

- Add packages for the approved backend modules:
  `identity`, `masterdata`, `importing`, `production`, `energy`, `quality`,
  `downtime`, `analytics`, `maintenance`, `reporting`, `audit`, and `shared`.
- Add standard layers for feature modules: `api`, `application`,
  `application.port`, `domain`, and `persistence`.
- Add executable architecture tests for module presence, layer presence,
  controller/service/domain/persistence boundaries, JPA boundary, and
  cross-module persistence isolation.

## Out of scope

- Business classes, controllers, services, repositories, entities, or domain
  behavior.
- Liquibase schema beyond the WP-S0-03 baseline.
- Frontend work.

## Current-state findings

- Backend currently has only the root application class plus PostgreSQL/
  Liquibase Testcontainers tests.
- `docs/ARCHITECTURE.md` fixes module names and dependency rules.
- No module/layer packages exist yet.

## Normative decisions and source conflicts

- DOCX v2.1 remains authoritative.
- `shared` is allowed as shared API/error/time primitives only and is not a
  feature module persistence owner.
- No source conflict was found.

## Milestones

### Milestone 1 - Module package skeleton

- [x] Add package markers for each required module and layer.
- [x] Keep markers behavior-free.

### Milestone 2 - Executable architecture rules

- [x] Add architecture-test dependency.
- [x] Add `ArchitectureRulesTest`.
- [x] Validate required package names and layer dependencies.

### Milestone 3 - Verification

- [x] Run `mvn "-Dtest=ArchitectureRulesTest" test`.
- [x] Run backend `mvn clean verify`.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Required modules exist | Architecture | `ArchitectureRulesTest` | Pass |
| Feature layers exist | Architecture | `ArchitectureRulesTest` | Pass |
| API does not use persistence/domain directly | Architecture | `ArchitectureRulesTest` | Pass |
| Application does not use persistence/API directly | Architecture | `ArchitectureRulesTest` | Pass |
| Domain is isolated from app/API/persistence | Architecture | `ArchitectureRulesTest` | Pass |
| Persistence does not leak into API or other modules | Architecture | `ArchitectureRulesTest` | Pass |

## Risks and rollback

- Data/migration risk: none.
- Security risk: none.
- Compatibility risk: architecture tests must avoid false positives from empty
  package markers while still catching future violations.
- Rollback or safe recovery: revert package marker files, test dependency, and
  `ArchitectureRulesTest`.

## Progress log

- 2026-07-06 - Started WP-S0-04 and confirmed module/layer rules from
  `docs/ARCHITECTURE.md`.
- 2026-07-06 - Added behavior-free package markers for 11 feature modules,
  shared support packages, and the standard feature layers.
- 2026-07-06 - Added ArchUnit dependency and `ArchitectureRulesTest` for
  package presence, layer boundaries, persistence isolation, naming rules, JPA
  REST boundary, and module cycles.
- 2026-07-06 - Fixed UTF-8 BOMs written by Windows PowerShell in generated
  `package-info.java` marker files.

## Decision log

- 2026-07-06 - Use package marker files plus architecture tests instead of
  placeholder business classes, so the skeleton adds no behavior.

## Unexpected findings

- Windows PowerShell `Set-Content -Encoding UTF8` wrote UTF-8 BOMs to generated
  Java marker files. `javac` rejected those files with illegal character
  errors, so the markers were re-encoded as UTF-8 without BOM.

## Final verification

- [x] Focused tests: `mvn "-Dtest=ArchitectureRulesTest" test` passed with 8
  tests, 0 failures, 0 errors.
- [x] Backend `clean verify` when affected: `mvn clean verify` passed with 10
  tests, 0 failures, 0 errors.
- [x] Frontend lint/test/build when affected: not affected by WP-S0-04.
- [x] Docker/health/E2E checks when affected: Testcontainers PostgreSQL started
  successfully during backend verification.
- [x] Diff reviewed for unrelated changes: no unrelated file edits made for
  WP-S0-04; repository is still pre-initial-commit with untracked project files.
- [x] Documentation and traceability updated: this execution plan updated.

## Handoff

WP-S0-04 is implemented and ready for Mohamed review.
