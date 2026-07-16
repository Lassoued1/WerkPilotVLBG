# WP-S2-08 — Import Benchmark Evidence (NFR-03)

Requirement: **NFR-03 — Import 100,000 rows within 3 minutes.**
Proof command: `.\mvnw.cmd "-Dtest=LargeCsvImportBenchmarkIT" verify`.

## Result

| Metric | Value |
| --- | --- |
| Rows imported | 100,000 (production template) |
| File size | 9,389,000 bytes (~9.0 MiB) |
| Elapsed (upload → COMMITTED) | **22,203 ms** |
| Throughput | ~4,503 rows/second |
| Budget | 180,000 ms |
| Margin | 8.1× under budget |
| Recorded at | 2026-07-16T07:13:51Z |

The measurement covers the complete synchronous pipeline: multipart upload,
strict CSV parsing/validation, master-data resolution, batched inserts of
100,000 `production_record` rows, and the job flip to `COMMITTED`. All
assertions passed: job `COMMITTED`, `valid_rows = 100000`, exactly 100,000
table rows for the job, elapsed below budget.

## Environment

| Component | Value |
| --- | --- |
| CPU | AMD Ryzen 5 5500U (6C/12T) |
| RAM | 17.8 GiB |
| OS | Windows 11 10.0 |
| JVM | Java 25 |
| Database | PostgreSQL 16 via Testcontainers (Docker daemon 29.6.1) |
| Warm-up | Four fixture-family imports in the same test class run before/around the benchmark |
| Dataset generator | `LargeCsvImportBenchmarkIT.GENERATOR_VERSION = "1"` — deterministic in-memory production CSV, hourly `[start,end)` windows from 2030-01-01T00:00Z, fixed master-data codes `BENCH-*`, 100 units/row, unique batch codes |
| Samples | 1 (single full import; NFR-03 is a single-import budget) |
| Failures | 0 (no 5xx, no assertion failures) |

## Performance changes required to meet NFR-03

Measured against the pre-change implementation (per-row `INSERT` plus 3–5
master-data lookups per row, i.e. 300k–500k queries for 100k rows), two
targeted changes were applied:

1. **Per-import master-data memoization** —
   `importing/application/csv/ImportMasterDataLookup.java`, used by all four
   CSV import services. Resolution semantics (active filter, German error
   messages) are unchanged; each distinct code is resolved once per import.
2. **Batched inserts (batch size 1000)** — `jdbcTemplate.batchUpdate` in the
   four measurement persistence adapters (production, energy, downtime,
   scrap).

## Fixture families (TEST-02)

`backend/src/test/resources/fixtures/imports/` holds the four valid/invalid
fixture pairs with expected counts (see its README). `LargeCsvImportBenchmarkIT`
asserts for every template: valid file → `COMMITTED`, 5 valid rows, 5 table
rows; invalid file → `FAILED`, 3 recorded German errors, 0 table rows.
