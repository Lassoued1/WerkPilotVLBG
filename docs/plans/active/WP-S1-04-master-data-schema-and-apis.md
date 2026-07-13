# Execution Plan: WP-S1-04 - Master-data Schema and APIs

- Status: Completed
- Owner: Codex / Mohamed
- Started: 2026-07-07
- Last updated: 2026-07-07
- Backlog item: WP-S1-04 Master-data schema and APIs
- Requirement IDs: S-02, MD-01..MD-08, ACC-02
- Source sections: 18, 24.6, 27.2
- Open question trace: OQ-004 resolved

## Goal and observable outcome

Implement backend master-data persistence and REST APIs for factories,
production lines, machines, products, shifts, downtime reasons, and scrap
categories. Authenticated users can read active master data. ADMIN can create,
update, and soft-delete records. Codes follow the resolved mixed uniqueness
rules: line and machine codes are factory-scoped; product, shift,
downtime-reason, and scrap-category codes are globally unique.

## In scope

- PostgreSQL/Liquibase schema with foreign keys and uniqueness constraints.
- Soft delete through `active=false`, preserving historical resolvability.
- ADMIN write endpoints and authenticated read endpoints.
- Fixed endpoint contracts for the Sprint 1 frontend work.
- Integration tests for CRUD, authorization, uniqueness, soft delete, and
  foreign-key/business-rule behavior.

## Out of scope

- Frontend master-data UI, owned by WP-S1-07 in VS Code.
- Import-time historical lookup and "unusable for new imports" enforcement,
  owned by Sprint 2 import validation.
- Audit query UI/API, owned by WP-S1-06 and later report/audit work.

## Endpoint contract

| Method | Path | Access | Behavior |
| --- | --- | --- | --- |
| GET, POST | `/factories` | Read authenticated; create ADMIN | List/create factories. |
| GET, PUT, DELETE | `/factories/{id}` | Read authenticated; write ADMIN | Read/update/soft-delete. |
| GET, POST | `/production-lines` | Read authenticated; create ADMIN | List/create factory-linked lines. |
| GET, PUT, DELETE | `/production-lines/{id}` | Read authenticated; write ADMIN | Read/update/soft-delete. |
| GET, POST | `/machines` | Read authenticated; create ADMIN | List/create line-linked machines. |
| GET, PUT, DELETE | `/machines/{id}` | Read authenticated; write ADMIN | Read/update/soft-delete. |
| GET, POST | `/products` | Read authenticated; create ADMIN | List/create products. |
| GET, PUT, DELETE | `/products/{id}` | Read authenticated; write ADMIN | Read/update/soft-delete. |
| GET, POST | `/shifts` | Read authenticated; create ADMIN | List/create shifts. |
| GET, PUT, DELETE | `/shifts/{id}` | Read authenticated; write ADMIN | Read/update/soft-delete. |
| GET, POST | `/downtime-reasons` | Read authenticated; create ADMIN | List/create reasons. |
| GET, PUT, DELETE | `/downtime-reasons/{id}` | Read authenticated; write ADMIN | Read/update/soft-delete. |
| GET, POST | `/scrap-categories` | Read authenticated; create ADMIN | List/create categories. |
| GET, PUT, DELETE | `/scrap-categories/{id}` | Read authenticated; write ADMIN | Read/update/soft-delete. |

## Milestones

### Milestone 1 - Schema

- [x] Add Liquibase migration for all master-data tables.
- [x] Add scoped/global uniqueness constraints.
- [x] Add foreign keys for factory/line/machine hierarchy.

### Milestone 2 - Backend API

- [x] Add persistence adapter.
- [x] Add application service with validation and soft delete.
- [x] Add REST controller DTOs for all master-data types.
- [x] Extend security rules for read-all/write-ADMIN behavior.

### Milestone 3 - Verification

- [x] Add `MasterDataCrudIT`.
- [x] Add `MasterDataUniquenessIT`.
- [x] Run focused proof.
- [x] Run backend gate.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| ADMIN can create/read/update/soft-delete all master data | Integration | `MasterDataCrudIT` | Pass |
| Non-admin authenticated users can read but not write | Integration | `MasterDataCrudIT` | Pass |
| Line codes are unique inside a factory only | Integration | `MasterDataUniquenessIT` | Pass |
| Machine codes are unique inside a factory only | Integration | `MasterDataUniquenessIT` | Pass |
| Product/shift/reason/category codes are globally unique | Integration | `MasterDataUniquenessIT` | Pass |
| Missing parent references are rejected | Integration | `MasterDataUniquenessIT` | Pass |

## Progress log

- 2026-07-07 - Started WP-S1-04 after WP-S1-03 backend gate passed.
- 2026-07-07 - Added master-data Liquibase schema for factories, production
  lines, machines, products, shifts, downtime reasons, and scrap categories.
- 2026-07-07 - Implemented application service, JDBC persistence adapter,
  REST controller DTOs, read-all/write-ADMIN security, and API contract updates.
- 2026-07-07 - Added integration coverage for CRUD, soft delete, read/write
  authorization, scoped uniqueness, global uniqueness, missing parents, and
  inactive parent rejection.
- 2026-07-07 - WP-S1-04 backend verification completed successfully.

## Final verification

- [x] Focused tests:
  `mvn "-Dtest=MasterDataCrudIT,MasterDataUniquenessIT" test` - 5 tests,
  0 failures, build success.
- [x] Targeted context/architecture/master-data tests:
  `mvn "-Dtest=ApplicationContextIT,ArchitectureRulesTest,MasterDataCrudIT,MasterDataUniquenessIT" test`
  - 16 tests, 0 failures, build success.
- [x] Backend `clean verify` when affected: `mvn clean verify` - 42 tests,
  0 failures, build success.
- [x] Frontend lint/test/build when affected: not run; WP-S1-04 is backend
  scope and no frontend source was edited.
- [x] Docker/health/E2E checks when affected: not run; Maven integration gate
  passed for the affected backend flow.
- [x] Diff reviewed for unrelated changes: repo content is currently untracked,
  so tracked git diff cannot isolate this work reliably.
- [x] Documentation and traceability updated: API contract and this plan updated.

## Handoff

Completed for backend/IntelliJ. Frontend master-data screens remain owned by
WP-S1-07 in VS Code.
