# Project Analysis

## Executive assessment

`WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx` is the final contractual source.
It contains 38 sections and the unchanged inventory of 106 explicit objective,
scope, exclusion, validation, functional, calculation, test, and acceptance
identifiers. All OQ-001..OQ-013 decisions and CR-001 are incorporated into the
normative text and logged in Section 36.3.

The MVP is ready for sprint-level implementation planning. No former OQ is an
open decision or a blocker. Remaining work consists of turning fixed behavior
into exact migrations, DTOs/OpenAPI, code, tests, runbooks, and evidence.

## Strong foundations

- Fixed MVP scope, 15 explicit exclusions, and automatic rejection rules.
- Java 25 / Spring Boot 4.1.x modular-monolith architecture and package rules.
- Five fixed roles with backend-enforced permissions.
- Four strict, asynchronous CSV imports and job-level correction/rollback.
- Explicit KPI formulas, energy granularity rules, and query-window semantics.
- Deterministic analytics with an identity key and auditable supersession.
- Complete maintenance lifecycle with optional due date and computed overdue.
- Fixed token, CSRF/CORS, email-reset, report-storage, retention, localization,
  frontend-library, and disclaimer decisions.
- UI routes, report sections, audit events, NFR targets, six sprint goals, and
  ten product acceptance criteria.
- A strict decision-support boundary with no industrial control.

## Main delivery risks

| Risk | Why it matters | Required control |
| --- | --- | --- |
| Contract drift | Behavior appears in prose, tables, APIs, screens, and the decision log. | Trace every task to IDs/sections and verify generated OpenAPI/tests. |
| Import memory/performance | Atomic validation of 100,000 rows can invite unsafe all-in-memory processing. | Use bounded parsing/staging and benchmark the real PostgreSQL path. |
| Time and granularity edges | DST, overnight shifts, contained intervals, and line/machine XOR affect every KPI. | Inject `Clock`; use fixed fixtures and database constraints. |
| Correction cascade | Import supersession must invalidate active measurements and rerun analytics without duplication. | Test correction/rollback, active-row filtering, and anomaly lineage together. |
| Security integration | Cookie refresh, CSRF, one-origin nginx, SMTP reset, and session revocation cross layers. | Implement one vertical security flow and integration-test browser/API behavior. |
| Report evidence lifecycle | A generated PDF must remain the original artifact even after later corrections. | UUID-only external storage, authorized download, backup, retention, and no silent regeneration. |
| Retention operation | Database rows and report files must be purged consistently without a scheduler. | Provide a safeguarded operator wrapper plus database script and audit proof. |
| Version compatibility | Major/minor versions are fixed while exact patch versions must work together. | Pin only verified managed dependencies in Sprint 0 and run full gates. |
| German UX consistency | Most UI text is frontend-owned, but CSV details and PDFs are backend-owned. | Central catalog, explicit exceptions, and exact-language tests. |

## Readiness by area

| Area | Readiness | Observation |
| --- | --- | --- |
| Product scope | High | Scope, exclusions, roles, workflows, and acceptance are fixed. |
| Architecture | High | Context, monolith, modules, layers, runtime, and deployment are prescribed. |
| Domain model | High for behavior | Invariants are fixed; exact non-normative columns still require reviewed migrations. |
| API | High for behavior | Main routes and shared shapes are fixed; supporting DTO details must be frozen in OpenAPI before coding each feature. |
| Security | High | Token transport, reset, CSRF/CORS, RBAC, audit, and retention posture are resolved. |
| Data import | High | Templates, limits, validation, async status, correction, and rollback are fixed. |
| Analytics | High | Formula, baseline, severity, identity, rerun, and recommendation behavior are fixed. |
| UI | High | Routes, German-language ownership, dependencies, and accessibility fallback are fixed. |
| Testing/acceptance | High | Required suites, gates, NFRs, and acceptance evidence are explicit. |
| Operations | Medium-high | Compose and operational outcomes are fixed; scripts and tested evidence remain to be built. |

## Implementation posture

1. Complete the final documentation alignment and approve the ordered backlog.
2. Build Sprint 0 as a thin, running architecture baseline with executable
   module/security/data rules.
3. Work one backlog task at a time in dependency order; never reopen resolved
   OQs or invent behavior outside the DOCX.
4. Deliver persistence, API, authorization, tests, and minimal UI together
   where applicable.
5. Keep KPI/anomaly logic in pure deterministic Java services and generate
   frontend types from OpenAPI.
6. Treat test output, performance measurements, and acceptance artifacts as
   required deliverables.
7. Stop for Mohamed's approval at every sprint boundary.

## Source authority

If this analysis or another derived file disagrees with v2.1, the DOCX wins.
Implementation status is never written back into the contractual document.
