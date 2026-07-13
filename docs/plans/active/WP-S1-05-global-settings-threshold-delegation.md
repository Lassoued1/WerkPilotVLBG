# Execution Plan: WP-S1-05 - Global Settings and Threshold Delegation

- Status: Completed
- Owner: Codex / Mohamed
- Started: 2026-07-07
- Last updated: 2026-07-07
- Backlog item: WP-S1-05 Global settings and threshold delegation
- Requirement IDs: MD-07, S-11
- Source sections: 8.2, 14.2, 18, 27.2
- Open question trace: OQ-003 resolved

## Goal and observable outcome

Implement the global `system_settings` row with
`energy_threshold_delegation_enabled=false` by default. ADMIN can read and
toggle the setting. Authenticated users can read it. ADMIN always passes the
backend energy-threshold write guard; ENERGY_MANAGER passes only while
delegation is enabled. Toggling the setting appends the required audit event.

## In scope

- Liquibase schema and default row for global settings.
- Read API for global settings.
- ADMIN-only update API for energy threshold delegation.
- Application-level guard for future energy-threshold writes.
- Audit event `THRESHOLD_DELEGATION_CHANGED`.
- Integration proof for default OFF, authorization, delegation behavior, and
  audit.

## Out of scope

- Full threshold CRUD/scope model, owned by WP-S4-01.
- Frontend settings UI, owned by WP-S1-07 in VS Code.
- Audit query API/UI, owned by WP-S1-06.

## Endpoint contract

| Method | Path | Access | Behavior |
| --- | --- | --- | --- |
| GET | `/settings/global` | Authenticated | Return global settings. |
| PUT | `/settings/global/energy-threshold-delegation` | ADMIN | Toggle delegation and audit the change. |

## Milestones

### Milestone 1 - Schema and ports

- [x] Add `system_settings` migration with default OFF.
- [x] Add settings port and persistence adapter.
- [x] Add threshold delegation audit event.

### Milestone 2 - API and authorization

- [x] Add settings service and controller.
- [x] Add ADMIN-only setting update security.
- [x] Add energy threshold write authorization guard.

### Milestone 3 - Verification

- [x] Add `EnergyThresholdDelegationIT`.
- [x] Run focused proof.
- [x] Run backend gate.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Delegation defaults OFF | Integration | `EnergyThresholdDelegationIT` | Pass |
| Only ADMIN toggles global setting | Integration | `EnergyThresholdDelegationIT` | Pass |
| ADMIN can always write thresholds | Integration | `EnergyThresholdDelegationIT` | Pass |
| ENERGY_MANAGER writes only when delegation is ON | Integration | `EnergyThresholdDelegationIT` | Pass |
| Toggle appends audit event | Integration | `EnergyThresholdDelegationIT` | Pass |

## Progress log

- 2026-07-07 - Started WP-S1-05 after WP-S1-04 backend gate passed.
- 2026-07-08 - Added singleton `system_settings` migration with
  `energy_threshold_delegation_enabled=false`.
- 2026-07-08 - Added global settings read/update API, ADMIN-only update rule,
  `THRESHOLD_DELEGATION_CHANGED` audit event, and energy-threshold write guard.
- 2026-07-08 - Focused proof initially found unsupported Liquibase
  `addCheckConstraint`; replaced it with explicit PostgreSQL SQL.
- 2026-07-08 - WP-S1-05 backend verification completed successfully.

## Final verification

- [x] Focused tests: `mvn "-Dtest=EnergyThresholdDelegationIT" test` -
  3 tests, 0 failures, build success.
- [x] Targeted context/architecture/delegation tests:
  `mvn "-Dtest=ApplicationContextIT,ArchitectureRulesTest,EnergyThresholdDelegationIT" test`
  - 14 tests, 0 failures, build success.
- [x] Backend `clean verify` when affected: `mvn clean verify` - 45 tests,
  0 failures, build success.
- [x] Frontend lint/test/build when affected: not run; WP-S1-05 is backend
  scope and no frontend source was edited.
- [x] Docker/health/E2E checks when affected: not run; Maven integration gate
  passed for the affected backend flow.
- [x] Diff reviewed for unrelated changes: repo content is currently untracked,
  so tracked git diff cannot isolate this work reliably.
- [x] Documentation and traceability updated: API contract and this plan updated.

## Handoff

Completed for backend/IntelliJ. Frontend settings/master-data screens remain
owned by WP-S1-07 in VS Code. Full threshold CRUD remains owned by WP-S4-01.
