# Implementation Backlog

## Use and status rules

- Implement one reviewable task at a time in dependency order.
- v2.1 is the authority; all OQ-001..OQ-013 items are resolved and none is a
  blocker. OQ labels below are traceability only.
- Every task records requirement IDs/sections, dependencies, deliverable, tests,
  exact commands, and remaining risk in its handoff.
- Use `Planned`, `Ready`, `In progress`, `In review`, or `Done`. `Done` requires
  focused proof plus the full applicable quality gates.
- Stop for Mohamed's approval after every sprint exit before starting the next.

## Common completion gates

After Sprint 0 creates the skeleton, every task must pass both affected full
gates, not only its focused test:

```powershell
# B — backend gate
cd backend
.\mvnw.cmd clean verify

# F — frontend gate
cd ..\frontend
npm ci
npm run lint
npm test -- --run
npm run build
```

Stack and acceptance gates used where stated:

```powershell
# D — deployment gate
docker compose config
docker compose up -d --build
curl.exe -f http://localhost:8080/actuator/health

# E — browser acceptance gate
cd frontend
npx playwright test
```

## Sprint 0 — Running architecture baseline

| Task | Status | Depends on | Source | Deliverable | Focused proof before B/F |
| --- | --- | --- | --- | --- | --- |
| WP-S0-01 Repository and toolchain skeleton | Ready | None | NFR-01, NFR-08, §29.1 | Git repository; Java 25 Maven Wrapper backend; strict TypeScript/Vite frontend; version checks. | `powershell -File scripts\validate-agent-kit.ps1` |
| WP-S0-02 Architecture and traceability baseline | Ready | None | §§6–12, 29; OOS-01..15 | Context, use-case, container, component, deployment, and package views; 106-ID traceability check. | `powershell -File scripts\validate-agent-kit.ps1` |
| WP-S0-03 PostgreSQL and Liquibase baseline | Planned | S0-01 | §§14–15; TEST-04 | PostgreSQL Testcontainer, root changelog, audit conventions, empty-database migration test, `ddl-auto=validate`. | `.\mvnw.cmd "-Dtest=LiquibaseMigrationIT" verify` |
| WP-S0-04 Modular package skeleton | Planned | S0-01, S0-02 | §§10–12 | Required `com.werkpilot` modules/layers and executable dependency rules. | `.\mvnw.cmd "-Dtest=ArchitectureRulesTest" test` |
| WP-S0-05 Shared API/error/OpenAPI foundation | Planned | S0-01, S0-04 | §§17, 24.6; VAL-01, NFR-06; OQ-009, OQ-013 | Shared error, pagination, aggregate, filter and job records; advice; OpenAPI 3.1 generation. | `.\mvnw.cmd "-Dtest=SharedApiContractIT" verify` |
| WP-S0-06 Frontend shell and approved libraries | Completed | S0-01, S0-05 | §§17.4, 25.2; NFR-07; OQ-010 | Router, React Query, forms, charts, i18n catalog, OpenAPI client, layouts and route smoke tests. | `npm test -- --run src/app` |
| WP-S0-07 Docker Compose, profiles and volumes | Planned | S0-03, S0-06 | §§13, 26.3, 29; NFR-01 | PostgreSQL, backend, nginx/frontend, local Mailpit, `report-files`, health checks, local/test/pilot profiles. | Gate D |
| WP-S0-08 CI, observability and seed contract | Planned | S0-03..07 | §§29.3, 34; NFR-05, NFR-08 | CI gates, JSON pilot logs, protected Actuator, injected clock, seed entry point and fixtures. | `.\mvnw.cmd "-Dtest=ApplicationContextIT" verify` plus B/F |

Sprint 0 exit: B, F, and D pass; migrations apply; architecture/security rules
are executable; the use-case and system architecture are reviewable. Stop for
approval.

## Sprint 1 — Identity and master data

| Task | Status | Depends on | Source | Deliverable | Focused proof before B/F |
| --- | --- | --- | --- | --- | --- |
| WP-S1-01 Authentication, refresh, logout and CSRF | Planned | S0-03, S0-05 | S-01; §§24.1, 27.1; OQ-001 | BCrypt, access JSON/Bearer token, hashed refresh identifiers, secure cookie, rotation, logout, CSRF and CORS. | `.\mvnw.cmd "-Dtest=AuthenticationFlowIT,RefreshTokenRotationIT,CsrfProtectionIT" verify` |
| WP-S1-02 Fixed RBAC and user administration | Planned | S1-01 | §§8, 8.1; TEST-03; ACC-02, ACC-09 | Five roles, backend authorization, user CRUD/status/role changes and audit. | `.\mvnw.cmd "-Dtest=UserAdministrationIT,SecurityAuthorizationIT" verify` |
| WP-S1-03 Emailed password reset | Planned | S1-01, S0-07 | §§24.1, 27.4, 29.2; OQ-002 | Enumeration-safe request, hashed 60-minute fragment token, Mailpit/SMTP, admin trigger, confirm and session revocation. | `.\mvnw.cmd "-Dtest=PasswordResetFlowIT" verify` |
| WP-S1-04 Master-data schema and APIs | Planned | S0-03, S1-02 | MD-01..08; §18; OQ-004 | Factories, lines, machines, products, shifts, reasons/categories; fixed uniqueness, FKs and soft delete. | `.\mvnw.cmd "-Dtest=MasterDataCrudIT,MasterDataUniquenessIT" verify` |
| WP-S1-05 Global settings and threshold delegation | Planned | S1-02, S1-04 | §§8.2, 14.2, 27.2; OQ-003 | `system_settings`, default-OFF delegation, ADMIN-only toggle, backend authorization and audit. | `.\mvnw.cmd "-Dtest=EnergyThresholdDelegationIT" verify` |
| WP-S1-06 Audit storage and query API | Planned | S1-01, S1-02 | S-11; §27.2; ACC-10 | Append-only persistence, ADMIN-only paginated query, required identity/role events. | `.\mvnw.cmd "-Dtest=AuditPersistenceIT,AuditAuthorizationIT" verify` |
| WP-S1-07 Identity/master-data UI | Planned | S0-06, S1-02..05 | §§18.1, 25; ACC-02 | German login/reset/admin/master-data screens, search, pagination, forms, permission states. | `npm test -- --run src/features/auth src/features/admin src/features/masterdata` |

Sprint 1 exit: login/refresh/logout/reset, five-role authorization, user/master
data administration, delegation, and audit pass B/F and focused tests. Stop for
approval.

## Sprint 2 — Asynchronous CSV import and correction

| Task | Status | Depends on | Source | Deliverable | Focused proof before B/F |
| --- | --- | --- | --- | --- | --- |
| WP-S2-01 Import job/error schema and async lifecycle | Done | S1-04, S0-05 | §§16, 24.6; OQ-009 | `PROCESSING/COMMITTED/FAILED/SUPERSEDED`, hash, limits, safe filename, 500-error cap, polling contract. | `.\mvnw.cmd "-Dtest=AsyncImportJobIT" verify` |
| WP-S2-02 Shared strict CSV parser/validator | Done | S2-01 | VAL-01..04; TEST-02; §§16.1, 17.4 | UTF-8/comma/dot/headers, UTC, master-data resolution, German row details, bounded processing. | `.\mvnw.cmd "-Dtest=CsvTemplateValidationTest" test` |
| WP-S2-03 Production import | Done | S2-02 | PRD-01; ACC-03, ACC-04 | Exact template, atomic commit/reject and traceability. | `.\mvnw.cmd "-Dtest=ProductionCsvImportIT" verify` |
| WP-S2-04 Energy import and granularity constraints | Done | S2-02 | ENE-01; §15.3; EC-01; OQ-005 | Exact template, machine/line XOR, one granularity per line/overlap, precision and atomicity. | `.\mvnw.cmd "-Dtest=EnergyCsvImportIT,EnergyGranularityValidationIT" verify` |
| WP-S2-05 Downtime and scrap imports | Done | S2-02 | DTS-01, DTS-04; ACC-03, ACC-04 | Exact templates, duration/reason/category validation and atomic commits. | `.\mvnw.cmd "-Dtest=DowntimeCsvImportIT,ScrapCsvImportIT" verify` |
| WP-S2-06 Correction and rollback | Done | S2-03..05 | §§16.6, 24.3; OQ-006 | COMMITTED-only target, atomic replacement, lineage, rollback reason, active-row filtering, audit and analytics hook. | `.\mvnw.cmd "-Dtest=CorrectionImportIT,ImportRollbackIT" verify` |
| WP-S2-07 Import UI and history | Planned | S0-06, S2-01..06 | S-03; §§24.6, 25; ACC-03, ACC-04 | Role-aware uploads, React Query polling, history, German details, correction/rollback ADMIN actions. | `npm test -- --run src/features/imports` |
| WP-S2-08 Fixtures and 100k benchmark | Planned | S2-03..06 | §34; NFR-03; TEST-02 | Four valid/invalid fixture families, expected counts and recorded benchmark. | `.\mvnw.cmd "-Dtest=LargeCsvImportBenchmarkIT" verify` |

Sprint 2 exit: all four templates pass atomic valid/invalid flows; correction
and rollback preserve lineage and cannot double KPIs; 100k evidence is recorded.
Stop for approval.

## Sprint 3 — KPI APIs and dashboard

| Task | Status | Depends on | Source | Deliverable | Focused proof before B/F |
| --- | --- | --- | --- | --- | --- |
| WP-S3-01 KPI calculation services | Planned | S2-03..06 | §4.2; PRD-02..05, ENE-02..04, DTS-02..05; TEST-01 | Pure production, energy/unit, scrap, downtime, availability and backlog calculations. | `.\mvnw.cmd "-Dtest=KpiCalculationServiceTest" test` |
| WP-S3-02 Time/filter/active-data query policy | Planned | S3-01 | §§16.6, 20.1, 24.6; EC-01..04; OQ-005 | Canonical `[from,to)` windows, fully contained rows, no proration, UTC/Vienna, COMMITTED-only queries. | `.\mvnw.cmd "-Dtest=TimeWindowAggregationIT,KpiFilteringIT" verify` |
| WP-S3-03 Production and energy APIs | Planned | S3-01, S3-02 | PRD-02..06, ENE-02..04, ENE-06 | Records, trends, output/hour, totals, energy/unit availability, top ten and evidence CSV. | `.\mvnw.cmd "-Dtest=ProductionApiIT,EnergyApiIT" verify` |
| WP-S3-04 Downtime and scrap APIs | Planned | S3-01, S3-02 | DTS-02..05 | Totals, exact Pareto/cumulative percentage, scrap rate and filters. | `.\mvnw.cmd "-Dtest=DowntimeApiIT,ScrapApiIT" verify` |
| WP-S3-05 Dashboard summary and performance | Planned | S3-03, S3-04 | S-09; NFR-02; ACC-05 | Shared filters/aggregates, cards/trends, indexes and 500k performance evidence. | `.\mvnw.cmd "-Dtest=DashboardApiIT,DashboardPerformanceIT" verify` |
| WP-S3-06 Dashboard and monitoring UI | Planned | S0-06, S3-05 | §25.1; ACC-05 | German cards/filters/charts/table fallbacks, machine/record views and traceability. | `npm test -- --run src/features/dashboard src/features/machines src/features/production` |

Sprint 3 exit: fixture KPIs match exactly, the browser never recalculates them,
and the 500k dashboard target passes. Stop for approval.

## Sprint 4 — Analytics and maintenance

| Task | Status | Depends on | Source | Deliverable | Focused proof before B/F |
| --- | --- | --- | --- | --- | --- |
| WP-S4-01 Threshold model and administration | Planned | S1-05, S3-02 | S-12, ENE-05; OQ-003 | Threshold CRUD/scope, global delegation authorization and audit. | `.\mvnw.cmd "-Dtest=ThresholdAdministrationIT" verify` |
| WP-S4-02 Deterministic anomaly engine | Planned | S3-03..05, S4-01 | AN-01, AN-03; ENE-05, DTS-06 | Baselines, threshold fallback, z-score boundaries, severity, explanation and import trigger. | `.\mvnw.cmd "-Dtest=AnomalyDetectionServiceTest" test` |
| WP-S4-03 Rerun identity and supersession | Planned | S2-06, S4-02 | AN-02, AN-04; §23.4; OQ-007 | ADMIN rerun, fixed identity, no-op/changed/disappeared behavior and linked history. | `.\mvnw.cmd "-Dtest=AnalyticsRerunIT,AnomalySupersessionIT" verify` |
| WP-S4-04 Recommendation templates and disclaimer | Planned | S4-02 | AN-05; TEST-05; §23.5; CR-001 | Versioned deterministic templates and exact German disclaimer carrier/tests. | `.\mvnw.cmd "-Dtest=RecommendationServiceTest" test` |
| WP-S4-05 Anomaly API and UI | Planned | S4-03, S4-04 | AN-03, AN-04; §§24.4, 25 | Filters/detail/status/audit, German display, disclaimer and ticket action. | `npm test -- --run src/features/anomalies` |
| WP-S4-06 Maintenance domain, overdue and APIs | Planned | S1-02, S1-04 | MNT-01..07; §22.2; OQ-012 | Lifecycle, assignment, comments, due date, computed overdue, ownership and audit. | `.\mvnw.cmd "-Dtest=MaintenanceTicketLifecycleIT,TicketOverdueCalculationTest" verify` |
| WP-S4-07 Anomaly-to-ticket and recurring pattern | Planned | S4-03, S4-06 | MNT-02; AN-04; ACC-07 | Linked creation/status, machine history and recurring-ticket detection. | `.\mvnw.cmd "-Dtest=AnomalyToTicketIT,RecurringTicketPatternIT" verify` |
| WP-S4-08 Maintenance UI | Planned | S0-06, S4-06, S4-07 | §25; ACC-07 | German assigned-first list/detail, transitions, notes, comments, overdue badge and history. | `npm test -- --run src/features/maintenance` |

Sprint 4 exit: abnormal fixtures create deterministic explainable anomalies,
reruns preserve history, and the approved ticket workflow passes. Stop for
approval.

## Sprint 5 — Reporting, operations, hardening, acceptance

| Task | Status | Depends on | Source | Deliverable | Focused proof before B/F |
| --- | --- | --- | --- | --- | --- |
| WP-S5-01 Monthly PDF and CSV evidence | Planned | S3-05, S4-04, S4-08 | BO-05; REP-01..03, REP-05; §26 | PDFBox fixed sections, metadata/filters, exact disclaimer and matching evidence CSV. | `.\mvnw.cmd "-Dtest=MonthlyReportServiceIT,PdfReportContentTest" verify` |
| WP-S5-02 Report storage and download | Planned | S0-07, S5-01 | REP-04; §26.3; OQ-008 | UUID-derived external file, authorized endpoint, `404` missing-file behavior, no silent regeneration. | `.\mvnw.cmd "-Dtest=ReportStorageIT,ReportDownloadSecurityIT" verify` |
| WP-S5-03 Reports and audit UI | Planned | S1-06, S5-02 | S-10, S-11; ACC-08, ACC-10 | Report generation/history/download and ADMIN-only audit screens. | `npm test -- --run src/features/reports src/features/audit` |
| WP-S5-04 Backup, restore and retention purge | Planned | S0-07, S5-02 | §§27.5, 28.1; OQ-011 | PostgreSQL/report backup, tested restore, safeguarded SQL plus operator wrapper, report purge and audit event. | `powershell -File scripts\test-backup-restore-purge.ps1` |
| WP-S5-05 Security and observability hardening | Planned | All protected APIs | §27; TEST-03; NFR-05, NFR-06; ACC-09 | Complete authorization matrix, sanitized errors, token/log review, headers/CORS/CSRF, protected Actuator/OpenAPI. | `.\mvnw.cmd "-Dtest=SecurityHardeningIT,SanitizedErrorResponseIT" verify` |
| WP-S5-06 Performance and concurrency | Planned | S2-08, S3-05, S5-02 | NFR-02..04 | 500k dashboard, 100k import, 25 users, recorded environment and no 5xx. | `k6 run tests\performance\pilot-load.js` |
| WP-S5-07 Browser and end-to-end acceptance | Planned | S5-03..06 | NFR-07; ACC-01..10 | Chrome/Edge/Firefox smoke and full login/import/dashboard/anomaly/ticket/report journey. | Gate E with configured browser projects |
| WP-S5-08 Handover documentation and evidence | Planned | S5-07 | §37; ACC-01..10 | README, `.env.example`, OpenAPI, CSV templates, runbooks, evidence index and completed checklist. | B, F, D, and E |

Sprint 5 exit: all mandatory acceptance criteria have reproducible evidence;
the pilot can be installed, backed up, restored, purged, operated, and reviewed.
Stop for final acceptance.
