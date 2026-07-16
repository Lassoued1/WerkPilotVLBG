# Import fixture families (WP-S2-08)

Deterministic valid/invalid fixture pairs for all four CSV templates
(TEST-02). `LargeCsvImportBenchmarkIT` creates the referenced master data
(factory `BENCH-F`, line `BENCH-L`, machine `BENCH-M`, product `BENCH-P`,
shift `BENCH-S`, downtime reason `BENCH-R`, scrap category `BENCH-C`) and
asserts the expected counts below. Structural-parser cases (missing headers,
bad numbers, >500 errors) are covered separately by
`CsvTemplateValidationTest` and the per-template import ITs; the invalid
files here exercise the semantic path (master-data resolution and interval
order), one error per row.

| File | Expected status | Expected valid rows | Expected error count | Committed table rows |
| --- | --- | --- | --- | --- |
| production-records.valid.csv | COMMITTED | 5 | 0 | 5 |
| production-records.invalid.csv | FAILED | 0 | 3 | 0 |
| energy-measurements.valid.csv | COMMITTED | 5 | 0 | 5 |
| energy-measurements.invalid.csv | FAILED | 0 | 3 | 0 |
| downtime-records.valid.csv | COMMITTED | 5 | 0 | 5 |
| downtime-records.invalid.csv | FAILED | 0 | 3 | 0 |
| scrap-records.valid.csv | COMMITTED | 5 | 0 | 5 |
| scrap-records.invalid.csv | FAILED | 0 | 3 | 0 |

Invalid-row composition (identical pattern per template):

1. Row 2: one unknown master-data code (factory/line/machine depending on the
   template) — German message "Der Stammdatencode ist unbekannt oder inaktiv.".
2. Row 3: `period_end` before `period_start` — "Das Ende muss nach dem Start
   liegen.".
3. Row 4: a second unknown code (shift/reason/category).

The 100k benchmark file is not stored here: it is generated in memory by
`LargeCsvImportBenchmarkIT` (see `GENERATOR_VERSION` in the test) to keep the
repository small and the dataset reproducible.
