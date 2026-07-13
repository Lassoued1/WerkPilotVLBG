# Execution Plan: WP-S1-06 - Audit Storage and Query API

- Status: In progress
- Owner: Codex / Mohamed
- Started: 2026-07-08
- Last updated: 2026-07-08
- Backlog item: WP-S1-06 Audit storage and query API
- Requirement IDs: S-11, ACC-10
- Source sections: 27.2, 31.1

## Goal and observable outcome

Complete backend audit support with append-only persistence and an ADMIN-only,
paginated query API. Existing identity, password-reset, and settings audit rows
remain queryable. The API supports stable filtering for event type, actor,
target, and occurrence window, and returns newest events first.

## In scope

- Audit event read model and query service.
- ADMIN-only REST API for audit events.
- Optional `trace_id` storage field and query response field.
- Pagination and filters using shared `PageResponse`.
- Required audit event vocabulary for Sprint 1 and later modules.
- Integration tests for persistence, filtering, pagination, and authorization.

## Out of scope

- Frontend audit UI, owned by later VS Code/frontend work.
- Retention purge implementation, owned by WP-S5-04.
- Import/ticket/report event producers, owned by their future work packages.

## Endpoint contract

| Method | Path | Access | Behavior |
| --- | --- | --- | --- |
| GET | `/audit-events` | ADMIN | Paginated newest-first audit query. |

Supported query parameters: `page`, `size`, `eventType`, `actorUserId`,
`targetUserId`, `from`, and `to`. `from` is inclusive; `to` is exclusive.

## Milestones

### Milestone 1 - Storage and port

- [ ] Add audit `trace_id` migration and read indexes.
- [ ] Add audit read model and search criteria.
- [ ] Extend audit port with paginated search.

### Milestone 2 - API and authorization

- [ ] Add audit query service.
- [ ] Add ADMIN-only `/audit-events` controller.
- [ ] Update security and API contract.

### Milestone 3 - Verification

- [ ] Add `AuditPersistenceIT`.
- [ ] Add `AuditAuthorizationIT`.
- [ ] Run focused proof.
- [ ] Run backend gate.
- [ ] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Appended audit events are persisted and queryable | Integration | `AuditPersistenceIT` | Pass |
| Query supports event type and actor/target filters | Integration | `AuditPersistenceIT` | Pass |
| Query supports occurrence window and pagination | Integration | `AuditPersistenceIT` | Pass |
| Audit query requires authentication | Integration | `AuditAuthorizationIT` | Pass |
| Audit query is ADMIN-only | Integration | `AuditAuthorizationIT` | Pass |

## Progress log

- 2026-07-08 - Started WP-S1-06 after WP-S1-05 backend gate passed.

## Final verification

- [ ] Focused tests:
- [ ] Backend `clean verify` when affected:
- [ ] Frontend lint/test/build when affected:
- [ ] Docker/health/E2E checks when affected:
- [ ] Diff reviewed for unrelated changes:
- [ ] Documentation and traceability updated:

## Handoff

Pending implementation.
