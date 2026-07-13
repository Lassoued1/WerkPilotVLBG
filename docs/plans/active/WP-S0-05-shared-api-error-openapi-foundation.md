# Execution Plan: WP-S0-05 - Shared API/error/OpenAPI Foundation

- Status: In review
- Owner: Codex / Mohamed
- Started: 2026-07-06
- Last updated: 2026-07-06
- Backlog item: WP-S0-05 Shared API/error/OpenAPI foundation
- Requirement IDs: VAL-01, NFR-06, OQ-009, OQ-013
- Source sections: 17, 24.6

## Goal and observable outcome

Create the backend shared API foundation for stable error envelopes, shared
response records, validation handling, and generated OpenAPI 3.1 contract
metadata.

## In scope

- Shared records for pagination, aggregate availability, typed filters, and
  asynchronous job status.
- Stable error codes and sanitized API error envelope.
- Global `@RestControllerAdvice` for application, validation, unreadable JSON,
  and unexpected errors.
- Backend-generated OpenAPI 3.1 document exposing shared schemas.
- Contract integration tests.

## Out of scope

- Business endpoints and authorization.
- Spring Security hardening for OpenAPI outside local development.
- Frontend OpenAPI client generation.
- CSV import persistence and job lifecycle.

## Milestones

### Milestone 1 - Shared API records

- [x] Add pagination, aggregate, filter, and job response records.
- [x] Add complete job status vocabulary.

### Milestone 2 - Error and validation foundation

- [x] Add stable error-code enum and error envelope.
- [x] Add global sanitized exception handling.
- [x] Add shared UTC clock bean for deterministic timestamp source injection.

### Milestone 3 - OpenAPI and verification

- [x] Add backend-generated OpenAPI 3.1 shared schema document.
- [x] Run `mvn "-Dtest=SharedApiContractIT" verify`.
- [x] Run backend `mvn clean verify`.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| OpenAPI 3.1 shared schemas exist | Integration | `SharedApiContractIT` | Pass |
| Shared shapes serialize with fixed names | Integration | `SharedApiContractIT` | Pass |
| CSV detail messages can be German | Integration | `SharedApiContractIT` | Pass |
| Top-level diagnostics stay English | Integration | `SharedApiContractIT` | Pass |
| Validation errors use `VALIDATION_FAILED` | Integration | `SharedApiContractIT` | Pass |
| Errors expose no stack traces/classes | Integration | `SharedApiContractIT` | Pass |

## Risks and rollback

- Dependency risk: Spring Boot 4 web artifacts may need Maven download.
- Security risk: OpenAPI protection is intentionally deferred to the security
  hardening task.
- Rollback or safe recovery: remove web/validation dependencies, shared classes,
  OpenAPI controller, contract test, and this plan.

## Progress log

- 2026-07-06 - Started WP-S0-05 after WP-S0-04.
- 2026-07-06 - Confirmed source scope from `docs/API_CONTRACT.md`,
  `docs/SECURITY.md`, `docs/ARCHITECTURE.md`, and OQ-009/OQ-013.
- 2026-07-06 - Added Spring Boot MVC, validation, and MVC test starters.
- 2026-07-06 - Added shared API records, stable error records/codes, global
  sanitized exception handling, UTC clock configuration, and OpenAPI 3.1 shared
  schema generation.
- 2026-07-06 - Added `SharedApiContractIT` for schema generation, shared shape
  serialization, validation errors, sanitized envelopes, and CSV detail
  handling.

## Decision log

- 2026-07-06 - Use a small internal OpenAPI 3.1 document generator for the
  Sprint 0 shared schemas, avoiding premature business endpoints.

## Unexpected findings

- `rg` is not available in this PowerShell environment; documentation search
  used native PowerShell instead.
- Spring Boot 4 moves MockMvc auto-configuration to
  `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` and
  requires `spring-boot-starter-webmvc-test`.
- Maven downloaded the Spring Boot 4 web MVC and validation artifacts from
  Maven Central during the first focused test run.

## Final verification

- [x] Focused tests: `mvn "-Dtest=SharedApiContractIT" verify` passed with 4
  tests, 0 failures, 0 errors.
- [x] Backend `clean verify` when affected: `mvn clean verify` passed with 14
  tests, 0 failures, 0 errors.
- [x] Frontend lint/test/build when affected: not affected by WP-S0-05.
- [x] Docker/health/E2E checks when affected: Testcontainers PostgreSQL started
  successfully during backend verification.
- [x] Diff reviewed for unrelated changes: no unrelated file edits made for
  WP-S0-05; repository is still pre-initial-commit with untracked project files.
- [x] Documentation and traceability updated: this execution plan updated.

## Handoff

WP-S0-05 is implemented and ready for Mohamed review.
