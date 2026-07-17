# Sprint 3 dashboard performance runbook

## Purpose

This runbook is the reusable evidence harness for WP-S3-05. It verifies that the
dashboard summary uses backend-calculated KPIs, applies shared filters, relies on
database indexes, and can be benchmarked on a pilot-sized dataset without moving
calculation logic into the frontend.

## Automated gate

Run from the repository root:

```powershell
cd backend
.\mvnw.cmd "-Dtest=DashboardApiIT,DashboardPerformanceIT" verify
.\mvnw.cmd clean verify
```

Expected result:

- `DashboardApiIT` proves dashboard cards/trends are calculated by the backend.
- `DashboardPerformanceIT` proves the Sprint 3 dashboard indexes are applied by
  Liquibase and the summary endpoint runs through the API harness.
- `clean verify` proves the change still satisfies backend architecture,
  migration, security, import and analytics gates.

## Pilot-scale benchmark procedure

Performance evidence is an environment measurement, not a fragile unit-test
timing assertion. For a 500,000-row pilot benchmark:

1. Start PostgreSQL with the same Docker/resource limits as the pilot machine.
2. Apply Liquibase from a clean database.
3. Import or seed 500,000 measurement rows across production, energy, downtime
   and scrap records, with realistic `COMMITTED` import jobs and representative
   factory/line/machine/shift filters.
4. Warm up `/dashboard/summary` once for the target window.
5. Record at least 20 timed samples of `GET /dashboard/summary`.
6. Store hardware/container limits, dataset composition, warm-up result,
   sample count, p50/p95/max latency, and failure count under `docs/evidence/`.

Acceptance target from `docs/TEST_STRATEGY.md`:

- dashboard summary under 2 seconds for demo data;
- under 5 seconds for 500,000 measurement rows;
- no `5xx`.

## Current automated proof

The automated proof covers correctness, query-index presence, endpoint security
and full backend integration. The external 500,000-row timing evidence should be
captured on the final pilot/CI environment because local laptop timings are not
portable evidence.
