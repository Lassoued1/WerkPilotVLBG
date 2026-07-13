# Test Strategy

## Quality objective

Generated code is accepted only with reproducible proof. The test pyramid
protects domain calculations and policies at low cost, verifies real
PostgreSQL/security behavior at integration level, and reserves browser tests
for critical workflows.

## Backend suites

| Suite | Tools | Mandatory coverage |
| --- | --- | --- |
| Unit | JUnit 5, AssertJ, Mockito only where useful | KPI formulas, validators, transition policies, z-scores, severity, recommendation templates. |
| Architecture | JUnit-based package/dependency checks | Controller/service/repository rules, module boundaries, no persistence leakage. |
| Persistence | Spring Boot Test, PostgreSQL Testcontainers | Liquibase, constraints, repositories, query aggregations, UTC/precision behavior. |
| API/security | MockMvc, Spring Security test | Requests, errors, pagination, every role, `401`, `403`, ownership restrictions. |
| Import | JUnit, MockMvc, Testcontainers, fixtures | Every valid/invalid template, strict headers, error cap, duplicate hash, atomicity, 100k benchmark. |
| Reporting | JUnit and PDF content/structure checks | Required sections, disclaimer, filters, authorization, deterministic fixed-clock metadata. |
| Operations | Script-level integration checks | Backup/restore, database and report-file retention safeguards, purge audit evidence. |

Do not use H2 as a substitute for the required PostgreSQL integration tests.

## Frontend suites

| Suite | Tools | Mandatory coverage |
| --- | --- | --- |
| Unit/component | Vitest, Testing Library | German labels, forms, state transitions, empty/loading/error states, permission-based visibility. |
| Contract | Generated OpenAPI types and API mocks | Request/response compatibility and stable error handling. |
| Accessibility | Testing Library plus approved automated checks | Semantics, labels, focus, keyboard use, chart table fallbacks. |
| End-to-end | Playwright | Login, import, validation error, dashboard, anomaly-to-ticket, report download. |
| Browser smoke | Current Chrome, Edge, Firefox | Login, dashboard, import, report. |

Frontend tests must not validate a duplicated KPI formula; they validate that
backend values are represented correctly.

## Required calculation cases

### Energy per unit

- normal matching energy/production;
- zero units -> `N/A`;
- missing production -> `N/A`;
- line-level versus machine-level input without double-counting;
- precision and rounding at display/report boundaries.
- machine/line XOR rejection and mixed granularity overlap rejection;
- independently aggregated energy/production intervals using fully contained
  rows and no proration.

### Scrap rate

- normal produced/scrap counts;
- zero total denominator;
- high scrap edge case;
- filters by machine, product, and shift.

### Availability and Pareto

- normal planned/downtime minutes;
- zero/missing planned minutes;
- downtime at period boundary;
- sorted reason totals and exact cumulative percentage.

### Anomalies

- 9 baseline periods -> threshold-only, quality `LOW`;
- 10 and 30 baseline periods;
- z-scores around 2.0, 3.0, and 4.0 boundaries;
- rerun idempotency;
- changed-result successor with `SUPERSEDED` predecessor;
- disappeared-result supersession without successor;
- user status not propagated to a successor;
- deterministic recommendation text/code/version.

### Identity and browser security

- login returns the access token in JSON and sets, but never serializes, the
  refresh token cookie;
- refresh rotates the cookie and requires the CSRF header;
- logout revokes/clears the cookie and requires the CSRF header;
- CORS allows only the configured frontend origin;
- password-reset request returns identical `202` responses for existing and
  unknown accounts and observes rate limits;
- reset token is hashed, fragment-carried, single-use, expires after 60
  minutes, and successful confirmation revokes every refresh token;
- raw passwords and tokens never appear in logs.

### Maintenance and delegation

- every allowed and forbidden ticket transition;
- `due_date` editable only for OPEN/IN_PROGRESS;
- overdue true/false around the Europe/Vienna business-date boundary and never
  represented as a status;
- ENERGY_MANAGER threshold writes allowed only while the global delegation
  setting is ON; ADMIN setting/threshold changes create the required events.

### Reports and retention

- exact German disclaimer on anomaly detail and in PDF content;
- UUID-derived external path, authorized download, and `404` without silent
  regeneration when the report file is missing;
- backup/restore includes PostgreSQL and `report-files`;
- manual purge refuses operational/audit data younger than 24/36 months,
  removes eligible report files, and records `RETENTION_PURGE_EXECUTED`.

## Import fixture matrix

For each of four templates include:

- minimal valid file;
- maximum field lengths;
- missing required header;
- unknown header;
- malformed UTF-8/invalid delimiter behavior;
- invalid timestamp/interval;
- unknown master-data code;
- negative numeric value;
- duplicate file hash;
- one invalid row among otherwise valid rows proving zero committed data;
- more than 500 errors proving cap and overflow metadata.
- upload returns `PROCESSING` and polling reaches `COMMITTED` or `FAILED`;
- correction of a COMMITTED job atomically supersedes target rows and reruns
  analytics;
- invalid correction is a no-op; rollback requires a reason and supersedes
  without replacement;
- KPI/report/export queries exclude rows belonging to SUPERSEDED jobs.

## Performance and concurrency

Record hardware/container limits, dataset generator version, database state,
warm-up, sample count, percentiles, and failure count.

- Dashboard summary: under 2 seconds for demo and under 5 seconds for 500,000
  measurement rows.
- Import 100,000 rows: target under 3 minutes.
- 25 concurrent authenticated users: no `5xx`.

Performance tests are evidence, not unit-test timing assertions.

## CI quality gates

Backend gate:

```text
./mvnw clean verify
```

It must compile with Java 25, apply Liquibase to PostgreSQL Testcontainers, and
run unit/integration/security/architecture tests.

Frontend gate:

```text
npm ci
npm run lint
npm test -- --run
npm run build
```

Required browser-security/report acceptance gate:

```text
npx playwright test
```

End-to-end and performance suites may be separate CI jobs but must be required
before final acceptance.

## Acceptance evidence

Store durable evidence under a release-specific `docs/evidence/` index or CI
artifacts. Do not commit secrets, raw tokens, private pilot data, database
dumps, or oversized generated reports.
