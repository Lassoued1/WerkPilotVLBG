# Execution Plan: WP-S0-02 - Architecture and Traceability Baseline

- Status: In review
- Owner: Codex / Mohamed
- Started: 2026-07-06
- Last updated: 2026-07-06
- Backlog item: WP-S0-02 Architecture and traceability baseline
- Requirement IDs: OOS-01..OOS-15
- Source sections: 6-12, 29

## Goal and observable outcome

Make the existing architecture and traceability baseline executable: the
repository validation must prove that required architecture views exist and
that the approved 106-ID requirement inventory remains complete and unique.

## In scope

- Validate the architecture document contains context, use-case, container,
  component, deployment/package views, module ownership, and safety exclusions.
- Validate the requirement inventory has exactly 106 unique IDs and the
  expected group counts.
- Validate OOS-01 through OOS-15 remain present.
- Record verification results in this plan.

## Out of scope

- Changing the contractual DOCX.
- Implementing backend modules, architecture tests, Docker, Liquibase, or API
  code. Those are later Sprint 0 tasks.
- Adding frontend screens or UI behavior.

## Current-state findings

- `docs/ARCHITECTURE.md` already contains system context, use-case, container
  and deployment, backend component, module ownership, and package dependency
  sections.
- `docs/ARCHITECTURE.md` contains five Mermaid diagrams.
- `docs/requirements/REQUIREMENTS.csv` contains 106 rows and 15
  `out_of_scope` entries.
- Existing Markdown has some mojibake in section symbols; this task does not
  rewrite contractual prose.

## Normative decisions and source conflicts

- DOCX v2.1 remains authoritative over derived Markdown.
- OQ-001..OQ-013 are resolved traceability labels, not blockers.
- No source conflict was found that blocks executable baseline validation.

## Milestones

### Milestone 1 - Traceability checks

- [x] Add validation for exact requirement inventory count.
- [x] Add validation for unique IDs and expected group counts.
- [x] Add validation for OOS-01..OOS-15.

### Milestone 2 - Architecture checks

- [x] Add validation for required architecture sections and Mermaid diagrams.
- [x] Add validation for approved module names and explicit no-control
  boundaries.
- [x] Add validation for package dependency rules.

### Milestone 3 - Verification

- [x] Run `powershell -File scripts\validate-agent-kit.ps1`.
- [x] Run affected backend/frontend gates if the skeleton remains touched.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Architecture views exist | Script | `scripts/validate-agent-kit.ps1` | Pass |
| 106 requirement IDs are indexed | Script | `scripts/validate-agent-kit.ps1` | Pass |
| Requirement IDs are unique | Script | `scripts/validate-agent-kit.ps1` | Pass |
| OOS-01..OOS-15 exist | Script | `scripts/validate-agent-kit.ps1` | Pass |
| Expected group counts match | Script | `scripts/validate-agent-kit.ps1` | Pass |

## Risks and rollback

- Data/migration risk: none.
- Security risk: none; no runtime behavior changes.
- Compatibility risk: low; validation script changes must remain compatible
  with Windows PowerShell.
- Rollback or safe recovery: revert the validation script additions and this
  plan if a later source rebaseline changes the architecture inventory.

## Progress log

- 2026-07-06 - Confirmed existing architecture and traceability documents cover
  the WP-S0-02 baseline.
- 2026-07-06 - Extended `scripts/validate-agent-kit.ps1` to validate required
  architecture sections, five Mermaid diagrams, module names, safety
  boundaries, package dependency rules, 106 unique requirement IDs, group
  counts, and OOS-01..OOS-15.
- 2026-07-06 - First focused run caught a PowerShell scalar-count issue in the
  OOS validation; corrected it with explicit array wrapping.
- 2026-07-06 - `powershell -File scripts\validate-agent-kit.ps1` passed and
  reported 106 unique IDs plus 5 Mermaid blocks.
- 2026-07-06 - `mvn clean verify` passed in `backend/` with `BUILD SUCCESS`.
- 2026-07-06 - `npm run lint`, `npm test -- --run`, and `npm run build`
  passed in `frontend/`.

## Decision log

- 2026-07-06 - Validate existing derived documentation instead of duplicating
  architecture prose, because `docs/ARCHITECTURE.md` and
  `docs/REQUIREMENTS_TRACEABILITY.md` already hold the reviewed baseline.

## Unexpected findings

- Backend verification emits a non-blocking Mockito/ByteBuddy dynamic-agent
  warning on Java 25. It does not fail WP-S0-02, but later test tasks should
  decide whether to configure Mockito as a Java agent.

## Final verification

- [x] Focused tests: `powershell -File scripts\validate-agent-kit.ps1` passed.
- [x] Backend `clean verify` when affected: `mvn clean verify` passed in
  `backend/`.
- [x] Frontend lint/test/build when affected: `npm run lint`,
  `npm test -- --run`, and `npm run build` passed in `frontend/`.
- [x] Docker/health/E2E checks when affected: not affected by WP-S0-02.
- [x] Diff reviewed for unrelated changes:
- [x] Documentation and traceability updated:

## Handoff

WP-S0-02 made the architecture and traceability baseline executable through
`scripts/validate-agent-kit.ps1`. The next smallest Sprint 0 task is WP-S0-03
PostgreSQL and Liquibase baseline, subject to Mohamed's ordering approval.
