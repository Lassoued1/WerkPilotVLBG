# Execution Plan: WP-S0-08 - CI, Observability and Seed Contract

- Status: In review
- Owner: Codex / Mohamed
- Started: 2026-07-07
- Last updated: 2026-07-07
- Backlog item: WP-S0-08 CI, observability and seed contract
- Requirement IDs: NFR-05, NFR-08
- Source sections: 29.3, 34

## Goal and observable outcome

Establish the Sprint 0 operational baseline: CI runs the required backend,
frontend, validation, and Compose configuration gates; pilot runtime settings
produce structured JSON logs and limited health exposure; and the backend has a
testable seed-data entry point tied to the documented demo fixtures without
creating business tables before Sprint 1.

## In scope

- GitHub Actions CI workflow for backend gate, frontend gate, agent-kit
  validation, and Compose config validation.
- Pilot profile logging and Actuator/OpenAPI exposure baseline.
- Backend seed contract classes and a guarded application runner.
- Backend tests proving injectable UTC clock, health endpoint, seed contract,
  and pilot logging configuration.
- Documentation of exact verification commands.

## Out of scope

- Frontend feature development; no frontend application source edits.
- Real identity/master-data persistence seeds before Sprint 1 schema.
- Full security hardening, authorization matrix, backup/restore, and E2E gates.

## Current-state findings

- `Clock` is already injected as a UTC system clock in `shared.time`.
- Actuator health exists from WP-S0-07, with only `health` exposed.
- Sample CSV fixtures and expected seed references exist in `docs/sample-data`.
- No `.github/workflows` CI exists yet.

## Normative decisions and source conflicts

- DOCX v2.1 remains authoritative.
- Seed contract is metadata-only in Sprint 0 because business persistence
  tables are not implemented yet.
- Actuator remains limited to health in Sprint 0; full protection is deferred
  to security tasks.
- No source conflict was found.

## Milestones

### Milestone 1 - CI workflow

- [x] Add required CI jobs for backend, frontend, validation, and Compose config.
- [x] Keep frontend commands as verification only; do not edit frontend app
  source.

### Milestone 2 - Observability profile

- [x] Add pilot profile with structured JSON console logging and limited
  management exposure.
- [x] Add tests that verify pilot observability properties.

### Milestone 3 - Seed contract

- [x] Add seed contract records and guarded runner.
- [x] Add tests proving the seed references match expected demo values and the
  runner stays disabled by default.

### Milestone 4 - Verification

- [x] Run focused backend tests.
- [x] Run backend gate.
- [x] Run frontend gate as verification only.
- [x] Run Compose config validation.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Application context exposes UTC clock | Integration | `ApplicationContextIT` | Pass |
| Health endpoint is available | Integration | `ApplicationContextIT` | Pass |
| Seed runner disabled by default | Integration | `ApplicationContextIT` | Pass |
| Seed contract has documented references | Unit | `SeedDataContractTest` | Pass |
| Pilot profile uses JSON logs and limited health exposure | Unit | `ObservabilityConfigurationTest` | Pass |

## Risks and rollback

- Data/migration risk: none; seed is metadata-only and guarded by property.
- Security risk: low; only health is exposed and secrets are not logged.
- Compatibility risk: CI Java 25 availability and Docker Compose syntax in CI.
- Rollback or safe recovery: remove workflow, seed classes/tests, pilot config,
  and this plan.

## Progress log

- 2026-07-07 - Started WP-S0-08 after WP-S0-07 reached review.
- 2026-07-07 - Added `.github/workflows/ci.yml` with validation, backend,
  frontend, and Compose config jobs.
- 2026-07-07 - Added `application-pilot.yml` with JSON-style console log
  pattern containing `traceId` and `userId`, and health-only management
  exposure.
- 2026-07-07 - Added `werkpilot.seed.enabled` default false, seed reference
  records, seed contract, and guarded seed runner.
- 2026-07-07 - Added `ApplicationContextIT`,
  `ObservabilityConfigurationTest`, and `SeedDataContractTest`.
- 2026-07-07 - Focused backend tests passed:
  `mvn "-Dtest=ApplicationContextIT,SeedDataContractTest,ObservabilityConfigurationTest" test`
  ran 7 tests with 0 failures and 0 errors.
- 2026-07-07 - Backend gate passed: `mvn clean verify` ran 21 tests with 0
  failures and 0 errors.
- 2026-07-07 - Agent-kit validation passed:
  `powershell -File scripts\validate-agent-kit.ps1`.
- 2026-07-07 - Compose config validation passed:
  `docker-compose --profile local config`.
- 2026-07-07 - Frontend gate was run as CI verification only. Initial `npm ci`
  failed because the previously launched Vite/esbuild process held
  `node_modules/@esbuild/win32-x64/esbuild.exe`; after stopping that local dev
  server, `npm ci`, `npm run lint`, `npm test -- --run`, and `npm run build`
  passed. Frontend source was not edited in WP-S0-08.

## Decision log

- 2026-07-07 - Keep seed contract metadata-only until Sprint 1 creates
  identity/master-data persistence.

## Unexpected findings

- A previously running Vite dev server locked `esbuild.exe` and blocked
  `npm ci` with EPERM. Stopping the local dev server cleared the lock.
- The frontend tests now report 4 passing tests, reflecting the VS Code-owned
  WP-S0-06 state present in the workspace.

## Final verification

- [x] Focused tests:
  `mvn "-Dtest=ApplicationContextIT,SeedDataContractTest,ObservabilityConfigurationTest" test`
  passed with 7 tests, 0 failures, 0 errors.
- [x] Backend `clean verify` when affected: `mvn clean verify` passed with 21
  tests, 0 failures, 0 errors.
- [x] Frontend lint/test/build when affected: verification only, no frontend
  source edits; after `npm ci`, `npm run lint`, `npm test -- --run`, and
  `npm run build` passed.
- [x] Docker/health/E2E checks when affected:
  `docker-compose --profile local config` passed. WP-S0-07 stack remained
  available from the previous task.
- [x] Diff reviewed for unrelated changes: repository remains pre-initial
  commit; WP-S0-08 changes are scoped to CI, backend config/classes/tests, and
  this plan. Frontend application source was not edited.
- [x] Documentation and traceability updated: this execution plan updated.

## Handoff

WP-S0-08 is implemented and ready for Mohamed review. Sprint 0 now has CI
workflow definitions, backend pilot observability configuration, health-only
management exposure, an injectable UTC clock proof, and a guarded metadata-only
seed contract tied to the documented demo fixtures. Sprint 0 exit review can
now focus on B/F/D gate evidence and Mohamed approval before Sprint 1.
