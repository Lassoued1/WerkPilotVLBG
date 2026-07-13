# Requirements Traceability

## Authority and inventory

`WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx` is the single authority.
`requirements/REQUIREMENTS.csv` indexes the unchanged 106 explicit IDs:

| Group | Count | Meaning |
| --- | ---: | --- |
| BO | 5 | Business objectives |
| S | 12 | In-scope modules |
| OOS | 15 | Explicit exclusions |
| VAL | 4 | Validation requirements |
| MD | 8 | Master-data requirements |
| PRD | 6 | Production requirements |
| ENE | 6 | Energy requirements |
| EC | 4 | Energy calculation rules |
| DTS | 6 | Downtime/scrap requirements |
| MNT | 7 | Maintenance requirements |
| AN | 5 | Analytics requirements |
| REP | 5 | Reporting requirements |
| NFR | 8 | Non-functional requirements |
| TEST | 5 | Minimum backend test requirements |
| ACC | 10 | Product acceptance criteria |
| **Total** | **106** | |

v2.1 added subsections and resolved decisions, not new numbered requirement
IDs. Mandatory unnumbered obligations remain traceable by section.

## Traceability chain

```text
DOCX requirement / normative section
  -> resolved decision where applicable
    -> backlog task
      -> execution plan
        -> code / migration / OpenAPI / runbook
          -> automated test
            -> acceptance evidence
```

Every task handoff lists its task ID, source IDs/sections, files and behavior,
migrations/API changes, tests, exact commands/results, risks, and next task.

## v2.1 decision coverage

| Decision | Normative implementation | Primary backlog tasks |
| --- | --- | --- |
| OQ-001 | Refresh cookie, access Bearer token, CSRF/CORS | WP-S1-01, WP-S5-05 |
| OQ-002 | Emailed one-time password reset | WP-S1-03 |
| OQ-003 | Global threshold-delegation setting | WP-S1-05, WP-S4-01 |
| OQ-004 | Mixed master-data uniqueness scopes | WP-S1-04 |
| OQ-005 | Energy XOR, granularity, contained windows | WP-S2-04, WP-S3-02 |
| OQ-006 | Job-level correction and rollback | WP-S2-06 |
| OQ-007 | Anomaly identity and supersession | WP-S4-03 |
| OQ-008 | External report storage and missing-file behavior | WP-S0-07, WP-S5-02, WP-S5-04 |
| OQ-009 | Shared API/job response shapes and async imports | WP-S0-05, WP-S2-01 |
| OQ-010 | Approved frontend dependency set | WP-S0-06 |
| OQ-011 | Manual safeguarded retention purge | WP-S5-04 |
| OQ-012 | Optional due date and computed overdue | WP-S4-06, WP-S4-08 |
| OQ-013 | English diagnostics, German catalog/CSV details | WP-S0-05, WP-S0-06, WP-S2-02 |
| CR-001 | Exact disclaimer in UI/tickets/PDF | WP-S4-04, WP-S4-05, WP-S5-01 |

These labels are resolved traceability pointers, never blockers.

## Coverage by sprint

| Sprint | Primary groups and sections | Acceptance contribution |
| --- | --- | --- |
| 0 | §§10–13, 17.4, 24.6, 25.2, 29; NFR-01, NFR-05, NFR-08, TEST-04 | ACC-01 foundation |
| 1 | S-01, S-02, S-11, MD-01..08, §§8, 27.1, 27.2, 27.4 | ACC-02, ACC-09, ACC-10 |
| 2 | S-03, VAL-01..04, PRD-01, ENE-01, DTS-01, DTS-04, §§16.1..16.6, TEST-02 | ACC-03, ACC-04 |
| 3 | PRD-02..06, ENE-02..04, ENE-06, EC-01..04, DTS-02..05, TEST-01 | ACC-05 |
| 4 | ENE-05, DTS-06, AN-01..05, MNT-01..07, §§22.2, 23.4, 23.5, TEST-05 | ACC-06, ACC-07 |
| 5 | REP-01..05, §§26.3, 27.5, 28.1, remaining NFR/TEST, all BO | ACC-01..10 final evidence |

## Acceptance evidence map

| Acceptance | Primary task(s) | Minimum evidence |
| --- | --- | --- |
| ACC-01 | WP-S0-07, WP-S5-07 | Compose services healthy; frontend reachable; backend health UP. |
| ACC-02 | WP-S1-02, WP-S1-04, WP-S1-07 | Admin login/master-data walkthrough and authorization tests. |
| ACC-03 | WP-S2-03..08 | Four COMMITTED jobs and expected database counts. |
| ACC-04 | WP-S2-02..08 | Rejected fixtures with German row/column/value detail and zero data rows. |
| ACC-05 | WP-S3-01..06 | Fixture calculations compared with API/UI and performance proof. |
| ACC-06 | WP-S4-02..05 | Known energy spike creates the expected active anomaly and explanation. |
| ACC-07 | WP-S4-06..08 | Linked ticket lifecycle, overdue behavior, and navigation. |
| ACC-08 | WP-S5-01..03 | Authorized original PDF, metadata, exact disclaimer, and content checks. |
| ACC-09 | WP-S1-01..02, WP-S5-05 | Complete role/controller and ownership authorization matrix. |
| ACC-10 | WP-S1-06, WP-S5-03..04 | Required events, ADMIN view, and purge audit evidence. |

## Status rules

- `Planned`: mapped to a backlog task whose dependencies are not complete.
- `In progress`: implementation or focused tests are incomplete.
- `Implemented`: code and focused tests pass.
- `Verified`: full gates and required acceptance evidence pass.
- `Deferred`: an approved rebaseline moves it outside the release.

Do not mark a requirement implemented because a class/screen merely exists.
Never write implementation status back into the contractual DOCX.
