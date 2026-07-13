# Product Baseline

## Purpose

WerkPilot VLBG gives manufacturing SMEs one auditable view of production,
energy, scrap, downtime, anomalies, and maintenance work. It is a pilot-ready
decision-support platform, not a machine-control or full factory execution
system.

## Fixed MVP modules

| Scope ID | Module | Required capability |
| --- | --- | --- |
| S-01 | Identity | Login, logout, refresh, users, fixed roles, self-service/admin-triggered password reset by emailed one-time link, profile. |
| S-02 | Master data | Factories, lines, machines, products, shifts, downtime reasons, scrap categories. |
| S-03 | CSV import | Upload, strict validation, error preview, atomic commit/rejection. |
| S-04 | Production | Units, machine/line/shift/product trends, traceability. |
| S-05 | Energy | kWh totals, energy per unit, top consumers, anomaly detection. |
| S-06 | Scrap/downtime | Rates, minutes, Pareto analysis, recurring patterns. |
| S-07 | Analytics | Thresholds, z-scores, severity, explanations, recommendations. |
| S-08 | Maintenance | Tickets, assignment, comments, transitions, anomaly linkage. |
| S-09 | Dashboard | Role-aware cards, filters, charts, anomalies. |
| S-10 | Reports | Monthly server-generated PDF and CSV evidence. |
| S-11 | Audit | Security, import, role, anomaly, ticket, and report events. |
| S-12 | Administration | Users, thresholds, shift calendar, and reference data. |

## Fixed roles

Persist exactly:

- `ADMIN`
- `PRODUCTION_MANAGER`
- `MAINTENANCE_TECHNICIAN`
- `ENERGY_MANAGER`
- `VIEWER`

The backend is the authorization authority. The frontend may hide unavailable
actions but cannot grant or infer permissions.

## Authoritative KPI formulas

| KPI | Formula | Edge behavior |
| --- | --- | --- |
| Total energy | `SUM(energy_kwh)` | Display kWh with one decimal. |
| Units produced | `SUM(units_produced)` | Integer. |
| Energy per unit | `SUM(energy_kwh) / NULLIF(SUM(units_produced), 0)` | `N/A` when units are zero or missing; two decimals. |
| Scrap rate | `SUM(scrap_count) / NULLIF(SUM(units_produced) + SUM(scrap_count), 0) * 100` | Two decimals. |
| Downtime | `SUM(downtime_min)` | Integer minutes. |
| Availability | `(planned_minutes - downtime_min) / planned_minutes * 100` | Requires imported/derived shift plan. |
| Anomaly count | Count non-dismissed anomalies | Group by severity where required. |
| Maintenance backlog | Count `OPEN` plus `IN_PROGRESS`; report the overdue sub-count | `overdue` is a query-time condition for an active ticket with a past optional `due_date`; it is not a status and must not be double-counted. |

Core KPI formulas must be implemented once in backend domain services. The
frontend formats backend results but does not recompute them.

## Anomaly baseline

- Energy baseline is per machine and shift.
- Use the previous 30 valid measurement periods, or all available periods when
  fewer than 30 exist.
- A statistical baseline requires at least 10 periods.
- With fewer than 10 periods, use only the configured absolute threshold and
  set `baseline_quality=LOW`.
- Z-score severity: `MEDIUM >= 2.0`, `HIGH >= 3.0`, and `CRITICAL >= 4.0` plus
  high absolute energy.
- Recommendations come from versioned templates and include an explanation,
  observed value, baseline value, detection method, template code, and version.

## CSV contract

- UTF-8, comma separator, dot decimal separator, mandatory header.
- Reject unknown columns and missing required columns.
- Maximum 25 MB and 100,000 rows.
- Store SHA-256 and reject duplicate hash for the same import type unless an
  explicit correction workflow is used.
- Parse ISO-8601 timestamps with an offset and store UTC instants.
- Validate all rows before commit. Any invalid row rejects the entire file.
- Return at most 500 row-level errors plus an overflow indicator.
- User-visible row errors are German.
- Imports are asynchronous: upload returns `PROCESSING`; polling ends at
  `COMMITTED` or `FAILED`.
- Correction targets one `COMMITTED` import job and atomically supersedes it;
  rollback supersedes without replacement. Active calculations include only
  rows whose job remains `COMMITTED`.

The exact templates are in `csv-templates/`.

## Fixed v2.1 decisions

- Refresh tokens use an HttpOnly, Secure, SameSite=Strict cookie; access tokens
  remain in JSON/Bearer headers. Refresh and logout require CSRF protection.
- Password reset uses a hashed, single-use, 60-minute token delivered by German
  email; local development uses Mailpit.
- Energy-threshold delegation is one global admin setting, default OFF.
- Line/machine codes are unique per factory; product/shift/reason/category
  codes are global.
- Energy rows reference exactly one machine or line, and one line cannot mix
  granularities for an overlapping period.
- Analytics reruns use the Section 23.4 identity key and preserve history with
  system-assigned `SUPERSEDED` anomalies.
- Reports are kept in the external `report-files` volume for 24 months and are
  downloaded only through authorization; missing files are not regenerated.
- Retention is a manual, safeguarded, audited operation; no scheduler exists.
- Backend diagnostics/codes are English, frontend UI is German, and dynamic
  CSV detail messages are the backend-owned German exception.
- The frontend runtime dependency list is fixed by Section 25.2.

## Decision-support disclaimer

Use this exact German text on anomaly details, ticket views that render linked
recommendations, and monthly PDF reports:

> „Hinweis: Diese Anwendung dient der Entscheidungsunterstützung. Empfehlungen
> sind keine zertifizierte Instandhaltungsdiagnose und ersetzen keine gesetzlich
> vorgeschriebenen Wartungs- und Sicherheitsverfahren.“

## Safety and scope exclusions

The MVP must not include:

- PLC, SCADA, HMI, OPC UA, MQTT, Modbus, or industrial gateway integration;
- machine shutdown, parameter changes, or any physical actuation;
- a Python analytics service, external LLM, or generative AI dependency;
- a full MES, ERP write-back, digital twin, native mobile app, or multi-tenant
  SaaS architecture;
- certified predictive maintenance or legal compliance certification;
- high-availability clustering, a data lake, or a general ABAC engine.

These exclusions are implementation constraints, not optional roadmap ideas.
