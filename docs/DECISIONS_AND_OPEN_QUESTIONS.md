# Resolved Decisions

The contractual authority is `WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx`.
Section 36.3 records the approved rationale and affected normative sections.
All former open questions OQ-001 through OQ-013 are resolved; none blocks
implementation. OQ identifiers remain only as traceability labels.

## Resolution register

| ID | Status | Approved resolution | Normative sections |
| --- | --- | --- | --- |
| OQ-001 | Resolved | The refresh token is transported only in an HttpOnly, Secure, SameSite=Strict cookie. Access tokens are returned in JSON and sent as Bearer tokens. CSRF protection applies to refresh and logout; CORS is restricted to the deployed frontend origin. | 13.1, 24.1, 27.1 |
| OQ-002 | Resolved | Self-service and admin-triggered password reset use the same German email flow with a hashed, single-use, 60-minute token carried in the URL fragment. Successful confirmation revokes all refresh tokens. | 6.1, 13.2, 24.1, 25, 27.2, 27.4, 29.2 |
| OQ-003 | Resolved | Energy-threshold delegation is one global `system_settings` boolean, default OFF. ADMIN always writes energy thresholds; ENERGY_MANAGER may write them only while delegation is ON. | 8.2, 14.2, 18 MD-07, 27.2 |
| OQ-004 | Resolved | Line and machine codes are unique within a factory. Product, shift, downtime-reason, and scrap-category codes are globally unique. | 18 MD-02..MD-06 |
| OQ-005 | Resolved | Each energy row references exactly one machine or line. A production line uses one energy granularity for an overlapping period. KPI windows aggregate fully contained rows without proration. | 15.3, 20.1 EC-01..EC-04 |
| OQ-006 | Resolved | Correction and rollback operate at import-job granularity. A valid correction atomically supersedes one COMMITTED job and links its replacement; rollback supersedes without replacement. Only COMMITTED job rows contribute to KPIs. | 14.1, 14.2, 15.3, 16.1, 16.6, 19.1, 24.3, 24.6, 27.2 |
| OQ-007 | Resolved | Analytics uses the fixed detection identity key defined in 23.4. Unchanged reruns are no-ops; changed or disappeared detections supersede historical anomalies. `SUPERSEDED` is terminal and system-assigned. | 23.3, 23.4 |
| OQ-008 | Resolved | Generated reports are stored in the external `report-files` volume outside the web root, downloaded only through the authorized endpoint, retained for 24 months, backed up, and never silently regenerated when missing. | 13.2, 24.5, 26.3, 28.1 |
| OQ-009 | Resolved | Pagination, aggregate availability, applied filters, and asynchronous job status use the four shared response shapes in 24.6. CSV uploads return `PROCESSING` and are polled to `COMMITTED` or `FAILED`. | 16, 24.6 |
| OQ-010 | Resolved | Approved frontend runtime libraries are `react-router`, `@tanstack/react-query`, `react-hook-form`, `recharts`, `i18next`, `react-i18next`, `openapi-typescript`, and `openapi-fetch`. No additional framework is implied. | 25.2 |
| OQ-011 | Resolved | Retention is a documented manual administrator operation. No scheduler is included. The operation enforces 24/36-month minima, purges report files, and records `RETENTION_PURGE_EXECUTED`. | 27.2, 27.3, 27.5, 28.1 |
| OQ-012 | Resolved | `OVERDUE` is not a ticket status. It is a query-time boolean for OPEN or IN_PROGRESS tickets whose optional `due_date` is before the Europe/Vienna business date. | 4.2, 14.2, 22.2 |
| OQ-013 | Resolved | `errorCode` and developer diagnostics are English. General user-visible text comes from the German frontend catalog. Dynamic CSV `details[].message` values are the backend-owned German exception. | 16.1, 17.1, 17.4 |
| CR-001 | Applied | The exact German decision-support disclaimer from 23.5 appears on anomaly details, ticket views that render recommendations, and monthly PDF reports, with frontend and PDF tests. | 23.5, 26.2 REP-02, 35.1 |

The German CSV-message requirement is sourced from Sections 16.1 and 17.4.
VAL-04 concerns comment length and plain-text storage; it is not the language
requirement.

## Change-control rule

Future changes to scope, architecture, KPI formulas, roles, CSV templates,
persistence, or out-of-scope boundaries follow Sections 36.1 and 36.2 of the
DOCX. A proposed change is not authoritative until the DOCX is rebaselined.
Derived Markdown files and local decision notes never override v2.1.
